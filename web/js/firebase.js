import { FIREBASE_PROJECT_DEFAULTS, STUDIO_CONTACT } from "./data.js";

const STORAGE_CONFIG_KEY = "star_artist_firebase_web_config_v1";
const FIREBASE_SDK_VERSION = "11.4.0";
const SDK_BASE = `https://www.gstatic.com/firebasejs/${FIREBASE_SDK_VERSION}`;

let sdkPromise = null;
let appInstance = null;
let authInstance = null;
let dbInstance = null;
let activeConfig = null;

function sanitizeWebConfig(raw) {
  if (!raw || typeof raw !== "object") return null;
  const apiKey = typeof raw.apiKey === "string" ? raw.apiKey.trim() : "";
  const appId = typeof raw.appId === "string" ? raw.appId.trim() : "";
  const projectId = (raw.projectId || FIREBASE_PROJECT_DEFAULTS.projectId).trim();
  if (!apiKey || !appId || !projectId) return null;

  return {
    apiKey,
    appId,
    projectId,
    authDomain: (raw.authDomain || `${projectId}.firebaseapp.com`).trim(),
    storageBucket: (raw.storageBucket || FIREBASE_PROJECT_DEFAULTS.storageBucket).trim(),
    messagingSenderId: (raw.messagingSenderId || FIREBASE_PROJECT_DEFAULTS.messagingSenderId).trim(),
    databaseId: (raw.databaseId || FIREBASE_PROJECT_DEFAULTS.databaseId).trim(),
    recaptchaSiteKey: typeof raw.recaptchaSiteKey === "string" ? raw.recaptchaSiteKey.trim() : ""
  };
}

export function getSavedManualWebConfig() {
  try {
    const raw = localStorage.getItem(STORAGE_CONFIG_KEY);
    if (!raw) return null;
    return sanitizeWebConfig(JSON.parse(raw));
  } catch {
    return null;
  }
}

export function saveManualWebConfig(config) {
  const clean = sanitizeWebConfig(config);
  if (!clean) {
    throw new Error("Please enter valid Firebase Web API Key, App ID, and Project ID.");
  }
  localStorage.setItem(STORAGE_CONFIG_KEY, JSON.stringify(clean));
  return clean;
}

export function clearManualWebConfig() {
  localStorage.removeItem(STORAGE_CONFIG_KEY);
}

export async function resolveFirebaseWebConfig() {
  // 1. Optional runtime global injected outside git (e.g., untracked firebase-env.js)
  if (typeof window !== "undefined" && window.__STAR_ARTIST_FIREBASE_CONFIG__) {
    const globalCfg = sanitizeWebConfig(window.__STAR_ARTIST_FIREBASE_CONFIG__);
    if (globalCfg) return globalCfg;
  }

  // 2. Firebase Hosting free Spark auto-provisioned config endpoint
  try {
    const res = await fetch("/__/firebase/init.json", { cache: "no-store" });
    if (res.ok) {
      const json = await res.json();
      const hostedCfg = sanitizeWebConfig(json);
      if (hostedCfg) return hostedCfg;
    }
  } catch {
    // Expected outside Firebase Hosting
  }

  // 3. Optional local user-configured Firebase Web App settings
  return getSavedManualWebConfig();
}

async function loadFirebaseModules() {
  if (!sdkPromise) {
    sdkPromise = Promise.all([
      import(`${SDK_BASE}/firebase-app.js`),
      import(`${SDK_BASE}/firebase-auth.js`),
      import(`${SDK_BASE}/firebase-firestore.js`),
      import(`${SDK_BASE}/firebase-app-check.js`)
    ]).then(([appMod, authMod, fsMod, appCheckMod]) => ({
      appMod,
      authMod,
      fsMod,
      appCheckMod
    }));
  }
  return sdkPromise;
}

export async function initFirebaseClient() {
  const cfg = await resolveFirebaseWebConfig();
  if (!cfg) {
    return { ready: false, reason: "missing_web_config", defaults: FIREBASE_PROJECT_DEFAULTS };
  }

  const { appMod, authMod, fsMod, appCheckMod } = await loadFirebaseModules();
  if (!appInstance) {
    const existing = appMod.getApps();
    appInstance = existing.length ? existing[0] : appMod.initializeApp(cfg);
    authInstance = authMod.getAuth(appInstance);
    // Explicitly target the same named Firestore database used by the Android app
    dbInstance = fsMod.getFirestore(appInstance, cfg.databaseId || FIREBASE_PROJECT_DEFAULTS.databaseId);
    activeConfig = cfg;

    if (cfg.recaptchaSiteKey) {
      try {
        appCheckMod.initializeAppCheck(appInstance, {
          provider: new appCheckMod.ReCaptchaV3Provider(cfg.recaptchaSiteKey),
          isTokenAutoRefreshEnabled: true
        });
      } catch (err) {
        console.warn("App Check initialization skipped:", err?.message || err);
      }
    }
  }

  return {
    ready: true,
    config: activeConfig,
    app: appInstance,
    auth: authInstance,
    db: dbInstance,
    ...((await loadFirebaseModules()))
  };
}

export async function subscribeAuthState(onChange) {
  const ctx = await initFirebaseClient();
  if (!ctx.ready) {
    onChange({ user: null, isAdmin: false, firebaseReady: false });
    return () => {};
  }

  return ctx.authMod.onAuthStateChanged(ctx.auth, async (user) => {
    if (!user) {
      onChange({ user: null, isAdmin: false, firebaseReady: true });
      return;
    }
    const admin = await checkIsAdmin(user);
    await ensureUserDocument(user);
    onChange({ user, isAdmin: admin, firebaseReady: true });
  });
}

export async function checkIsAdmin(user) {
  if (!user) return false;
  if (
    user.email &&
    user.email.toLowerCase() === STUDIO_CONTACT.adminEmail.toLowerCase() &&
    user.emailVerified
  ) {
    return true;
  }
  const ctx = await initFirebaseClient();
  if (!ctx.ready) return false;
  try {
    const adminRef = ctx.fsMod.doc(ctx.db, "admins", user.uid);
    const snap = await ctx.fsMod.getDoc(adminRef);
    return snap.exists();
  } catch {
    return false;
  }
}

async function ensureUserDocument(user) {
  const ctx = await initFirebaseClient();
  if (!ctx.ready || !user) return;
  const { fsMod, db } = ctx;
  const userRef = fsMod.doc(db, "users", user.uid);
  try {
    const existing = await fsMod.getDoc(userRef);
    if (!existing.exists()) {
      await fsMod.setDoc(userRef, {
        userId: user.uid,
        email: user.email || "",
        displayName: user.displayName || "Star Artist Member",
        photoUrl: user.photoURL || "",
        role: "user",
        announcementsOptIn: true,
        appointmentPushOptIn: true,
        createdAt: fsMod.serverTimestamp(),
        updatedAt: fsMod.serverTimestamp()
      });
    }
  } catch (err) {
    console.warn("Unable to sync user profile doc:", err?.message || err);
  }
}

export async function syncWorkshopNewsletterOptIn() {
  const ctx = await initFirebaseClient();
  const user = ctx?.auth?.currentUser;
  if (!ctx.ready || !user) return false;
  const { fsMod, db } = ctx;
  try {
    await fsMod.updateDoc(fsMod.doc(db, "users", user.uid), {
      announcementsOptIn: true,
      updatedAt: fsMod.serverTimestamp()
    });
    return true;
  } catch {
    return false;
  }
}

export async function signInWithGooglePopup() {
  const ctx = await initFirebaseClient();
  if (!ctx.ready) throw new Error("Firebase Web App is not configured yet.");
  const provider = new ctx.authMod.GoogleAuthProvider();
  provider.setCustomParameters({ prompt: "select_account" });
  return ctx.authMod.signInWithPopup(ctx.auth, provider);
}

export async function signInWithEmailPassword(email, password) {
  const ctx = await initFirebaseClient();
  if (!ctx.ready) throw new Error("Firebase Web App is not configured yet.");
  return ctx.authMod.signInWithEmailAndPassword(ctx.auth, email.trim(), password);
}

export async function registerWithEmailPassword(email, password) {
  const ctx = await initFirebaseClient();
  if (!ctx.ready) throw new Error("Firebase Web App is not configured yet.");
  const cred = await ctx.authMod.createUserWithEmailAndPassword(ctx.auth, email.trim(), password);
  try {
    await ctx.authMod.sendEmailVerification(cred.user);
  } catch {
    // Verification email optional fallback
  }
  return cred;
}

export async function signOutFirebase() {
  const ctx = await initFirebaseClient();
  if (!ctx.ready) return;
  await ctx.authMod.signOut(ctx.auth);
}

export async function fetchApprovedGallery() {
  const ctx = await initFirebaseClient();
  if (!ctx.ready) return [];
  const { fsMod, db } = ctx;
  const q = fsMod.query(
    fsMod.collection(db, "gallery"),
    fsMod.where("status", "==", "approved")
  );
  const snap = await fsMod.getDocs(q);
  return snap.docs
    .map((d) => ({ id: d.id, ...d.data() }))
    .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
}

export async function fetchMyBookings(uid) {
  const ctx = await initFirebaseClient();
  if (!ctx.ready || !uid) return [];
  const { fsMod, db } = ctx;
  const q = fsMod.query(
    fsMod.collection(db, "bookings"),
    fsMod.where("userId", "==", uid)
  );
  const snap = await fsMod.getDocs(q);
  return snap.docs
    .map((d) => ({ id: d.id, ...d.data() }))
    .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
}

export async function createShootBooking(payload) {
  const ctx = await initFirebaseClient();
  if (!ctx.ready) throw new Error("Firebase Web App is not configured.");
  const user = ctx.auth.currentUser;
  if (!user) throw new Error("Please sign in first to submit a booking.");

  const { fsMod, db } = ctx;
  const docData = {
    userId: user.uid,
    clientName: payload.clientName.trim(),
    contactNumber: payload.contactNumber.trim(),
    serviceType: payload.serviceType.trim(),
    preferredDate: payload.preferredDate.trim(),
    preferredTime: payload.preferredTime.trim() || "11:00 AM",
    location: payload.location.trim() || "Studio (Talwara Khurd)",
    notes: payload.notes.trim(),
    status: "Pending",
    fcmToken: "",
    createdAt: fsMod.serverTimestamp(),
    updatedAt: fsMod.serverTimestamp()
  };
  const ref = await fsMod.addDoc(fsMod.collection(db, "bookings"), docData);
  return ref.id;
}

export async function getBookingSyncContext() {
  const projectId = activeConfig?.projectId || FIREBASE_PROJECT_DEFAULTS.projectId;
  const databaseId = activeConfig?.databaseId || FIREBASE_PROJECT_DEFAULTS.databaseId;
  const user = authInstance?.currentUser || null;
  if (!user) {
    return { uid: "", idToken: "", projectId, databaseId };
  }
  let idToken = "";
  try {
    // Use cached ID token (forceRefresh=false) so queueing succeeds even while completely offline
    idToken = await user.getIdToken(false);
  } catch {
    idToken = user.accessToken || "";
  }
  return { uid: user.uid, idToken, projectId, databaseId };
}

export async function cancelMyBooking(bookingId) {
  const ctx = await initFirebaseClient();
  if (!ctx.ready || !ctx.auth.currentUser) throw new Error("Sign in required.");
  const { fsMod, db } = ctx;
  await fsMod.updateDoc(fsMod.doc(db, "bookings", bookingId), {
    status: "Cancelled",
    updatedAt: fsMod.serverTimestamp()
  });
}

export async function fetchMyModels(uid) {
  const ctx = await initFirebaseClient();
  if (!ctx.ready || !uid) return [];
  const { fsMod, db } = ctx;
  const q = fsMod.query(
    fsMod.collection(db, "models"),
    fsMod.where("userId", "==", uid)
  );
  const snap = await fsMod.getDocs(q);
  return snap.docs
    .map((d) => ({ id: d.id, ...d.data() }))
    .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
}

export async function submitModelRegistration(payload) {
  const ctx = await initFirebaseClient();
  if (!ctx.ready) throw new Error("Firebase Web App is not configured.");
  const user = ctx.auth.currentUser;
  if (!user) throw new Error("Please sign in first to apply as an 18+ model.");

  const ageNum = Number.parseInt(payload.age, 10);
  if (!Number.isInteger(ageNum) || ageNum < 18 || ageNum > 100) {
    throw new Error("Applicant must be strictly 18 years of age or older.");
  }
  if (!payload.consentGiven) {
    throw new Error("18+ consent verification is mandatory.");
  }

  const { fsMod, db } = ctx;
  const ref = await fsMod.addDoc(fsMod.collection(db, "models"), {
    userId: user.uid,
    fullName: payload.fullName.trim(),
    age: ageNum,
    gender: payload.gender || "Female",
    city: payload.city.trim(),
    phoneNumber: payload.phoneNumber.trim(),
    skills: payload.skills.trim(),
    portfolioUrls: payload.portfolioUrls.trim(),
    consentGiven: true,
    status: "pending",
    adminNotes: "",
    createdAt: fsMod.serverTimestamp(),
    updatedAt: fsMod.serverTimestamp()
  });
  return ref.id;
}

export async function submitGalleryUpload(payload, autoApprove = false) {
  const ctx = await initFirebaseClient();
  if (!ctx.ready) throw new Error("Firebase Web App is not configured.");
  const user = ctx.auth.currentUser;
  if (!user) throw new Error("Please sign in first to submit gallery artwork.");

  const { fsMod, db } = ctx;
  const ref = await fsMod.addDoc(fsMod.collection(db, "gallery"), {
    userId: user.uid,
    title: payload.title.trim(),
    category: payload.category || "Fashion",
    mediaType: payload.mediaType || "photo",
    mediaUrl: payload.mediaUrl.trim(),
    thumbnailUrl: payload.mediaUrl.trim(),
    isDemo: false,
    status: autoApprove ? "approved" : "pending",
    createdAt: fsMod.serverTimestamp(),
    updatedAt: fsMod.serverTimestamp()
  });
  return ref.id;
}

export async function fetchAnnouncements() {
  const ctx = await initFirebaseClient();
  if (!ctx.ready || !ctx.auth.currentUser) return [];
  const { fsMod, db } = ctx;
  const snap = await fsMod.getDocs(fsMod.collection(db, "announcements"));
  return snap.docs
    .map((d) => ({ id: d.id, ...d.data() }))
    .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
}

export async function fetchServiceReviews() {
  const ctx = await initFirebaseClient();
  if (!ctx.ready || !ctx.auth.currentUser) return [];
  const { fsMod, db } = ctx;
  const snap = await fsMod.getDocs(fsMod.collection(db, "reviews"));
  return snap.docs
    .map((d) => ({ id: d.id, ...d.data() }))
    .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
}

export async function submitCatalogReview({ packageId, serviceType, rating, comment }) {
  const ctx = await initFirebaseClient();
  if (!ctx.ready) throw new Error("Firebase Web App is not configured.");
  const user = ctx.auth.currentUser;
  if (!user) throw new Error("Please sign in first to post a star rating and comment.");

  const cleanComment = String(comment ?? "").trim();
  const numRating = Number(rating);
  if (!Number.isInteger(numRating) || numRating < 1 || numRating > 5) {
    throw new Error("Please select a star rating between 1 and 5.");
  }
  if (cleanComment.length < 5 || cleanComment.length > 1000) {
    throw new Error("Comment must be between 5 and 1000 characters.");
  }

  const cleanPackageId = String(packageId ?? "").trim();
  const bookingId = `catalog_${cleanPackageId}`;
  const reviewId = `${user.uid}_${bookingId}`;
  const { fsMod, db } = ctx;
  const reviewRef = fsMod.doc(db, "reviews", reviewId);
  const existing = await fsMod.getDoc(reviewRef);
  const reviewerName = (user.displayName || user.email?.split("@")[0] || "Studio Member").trim().slice(0, 100);
  const safeServiceType = String(serviceType || "Studio Session").trim().slice(0, 100);

  if (!existing.exists()) {
    await fsMod.setDoc(reviewRef, {
      userId: user.uid,
      bookingId,
      reviewerName: reviewerName.length >= 2 ? reviewerName : "Studio Member",
      reviewerAvatarUrl: user.photoURL || "",
      serviceType: safeServiceType,
      rating: numRating,
      comment: cleanComment,
      createdAt: fsMod.serverTimestamp(),
      updatedAt: fsMod.serverTimestamp()
    });
  } else {
    await fsMod.updateDoc(reviewRef, {
      rating: numRating,
      comment: cleanComment,
      reviewerName: reviewerName.length >= 2 ? reviewerName : "Studio Member",
      reviewerAvatarUrl: user.photoURL || "",
      updatedAt: fsMod.serverTimestamp()
    });
  }
  return reviewId;
}

// --- ADMIN-ONLY OPERATIONS (Enforced server-side by firestore.rules) ---

export async function adminFetchDashboardData() {
  const ctx = await initFirebaseClient();
  if (!ctx.ready || !ctx.auth.currentUser) throw new Error("Admin sign-in required.");
  const { fsMod, db } = ctx;
  const [bookingsSnap, modelsSnap, gallerySnap, announcementsSnap] = await Promise.all([
    fsMod.getDocs(fsMod.collection(db, "bookings")),
    fsMod.getDocs(fsMod.collection(db, "models")),
    fsMod.getDocs(fsMod.collection(db, "gallery")),
    fsMod.getDocs(fsMod.collection(db, "announcements"))
  ]);
  const mapSorted = (snap) =>
    snap.docs
      .map((d) => ({ id: d.id, ...d.data() }))
      .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
  return {
    bookings: mapSorted(bookingsSnap),
    models: mapSorted(modelsSnap),
    gallery: mapSorted(gallerySnap),
    announcements: mapSorted(announcementsSnap)
  };
}

export async function adminUpdateBookingStatus(bookingId, status) {
  const ctx = await initFirebaseClient();
  const { fsMod, db } = ctx;
  await fsMod.updateDoc(fsMod.doc(db, "bookings", bookingId), {
    status,
    updatedAt: fsMod.serverTimestamp()
  });
}

export async function adminDeleteBooking(bookingId) {
  const ctx = await initFirebaseClient();
  const { fsMod, db } = ctx;
  await fsMod.deleteDoc(fsMod.doc(db, "bookings", bookingId));
}

export async function adminUpdateModelStatus(modelId, status, adminNotes = "") {
  const ctx = await initFirebaseClient();
  const { fsMod, db } = ctx;
  await fsMod.updateDoc(fsMod.doc(db, "models", modelId), {
    status,
    adminNotes: adminNotes.trim(),
    updatedAt: fsMod.serverTimestamp()
  });
}

export async function adminUpdateGalleryStatus(itemId, status) {
  const ctx = await initFirebaseClient();
  const { fsMod, db } = ctx;
  await fsMod.updateDoc(fsMod.doc(db, "gallery", itemId), {
    status,
    updatedAt: fsMod.serverTimestamp()
  });
}

export async function adminDeleteGalleryItem(itemId) {
  const ctx = await initFirebaseClient();
  const { fsMod, db } = ctx;
  await fsMod.deleteDoc(fsMod.doc(db, "gallery", itemId));
}

export async function adminPublishAnnouncement({ title, message, category, priority }) {
  const ctx = await initFirebaseClient();
  const user = ctx.auth.currentUser;
  if (!user) throw new Error("Admin sign-in required.");
  const { fsMod, db } = ctx;
  const id = `ann_${Date.now()}`;
  await fsMod.setDoc(fsMod.doc(db, "announcements", id), {
    authorId: user.uid,
    authorName: user.displayName || STUDIO_CONTACT.studioName,
    title: title.trim(),
    message: message.trim(),
    category: category || "Studio Update",
    priority: priority || "normal",
    targetTopic: "studio_announcements",
    actionRoute: "home",
    createdAt: fsMod.serverTimestamp(),
    updatedAt: fsMod.serverTimestamp()
  });
}
