import { FIREBASE_PROJECT_DEFAULTS, STUDIO_CONTACT } from "./data.js";
import { buildStorageObjectPath, computeUploadProgress } from "./image-upload.js";
import { isAuthorizedAdminUser } from "./admin-guard.js";
import { sanitizePublicModelProfile } from "./company-booking.js";

const LEGACY_STORAGE_CONFIG_KEY = "star_artist_firebase_web_config_v1";
const FIREBASE_SDK_VERSION = "11.4.0";
const SDK_BASE = `https://www.gstatic.com/firebasejs/${FIREBASE_SDK_VERSION}`;

let sdkPromise = null;
let storageSdkPromise = null;
let appInstance = null;
let authInstance = null;
let dbInstance = null;
let activeConfig = null;

export function sanitizeWebConfig(raw) {
  if (!raw || typeof raw !== "object") return null;
  const apiKey = typeof raw.apiKey === "string" ? raw.apiKey.trim() : "";
  const appId = typeof raw.appId === "string" ? raw.appId.trim() : "";
  const projectId = String(raw.projectId || FIREBASE_PROJECT_DEFAULTS.projectId).trim();
  if (!apiKey || !appId || !projectId) return null;

  return {
    apiKey,
    appId,
    projectId,
    authDomain: String(raw.authDomain || `${projectId}.firebaseapp.com`).trim(),
    storageBucket: String(raw.storageBucket || FIREBASE_PROJECT_DEFAULTS.storageBucket).trim(),
    messagingSenderId: String(raw.messagingSenderId || FIREBASE_PROJECT_DEFAULTS.messagingSenderId).trim(),
    databaseId: String(raw.databaseId || FIREBASE_PROJECT_DEFAULTS.databaseId).trim(),
    recaptchaSiteKey: typeof raw.recaptchaSiteKey === "string" ? raw.recaptchaSiteKey.trim() : ""
  };
}

/**
 * Removes any legacy per-device localStorage Firebase config saved by older builds
 * so every mobile device always uses the permanent shared studio Firebase project.
 */
export function clearLegacyLocalWebConfig() {
  try {
    if (typeof localStorage !== "undefined") {
      localStorage.removeItem(LEGACY_STORAGE_CONFIG_KEY);
    }
  } catch {
    // Ignore restricted storage contexts
  }
}

async function fetchJsonConfig(url, { fetchImpl = globalThis.fetch, cache = "no-store" } = {}) {
  if (typeof fetchImpl !== "function") return null;
  try {
    const res = await fetchImpl(url, { cache });
    if (!res || !res.ok) return null;
    const contentType = String(res.headers?.get?.("content-type") || "").toLowerCase();
    // Guard against SPA HTML fallback responses when outside Firebase Hosting
    if (contentType && !contentType.includes("json") && !contentType.includes("text/plain")) {
      return null;
    }
    const json = await res.json();
    return sanitizeWebConfig(json);
  } catch {
    return null;
  }
}

/**
 * Permanently resolves the shared Firebase Web App configuration for all devices:
 * 1. Automatic Firebase Hosting auto-init endpoint (/__/firebase/init.json) on free Spark plan
 * 2. Centralized shared web configuration (/firebase-config.json) for custom domains, mobile previews, and offline PWA cache
 * 3. Optional runtime global override (window.__STAR_ARTIST_FIREBASE_CONFIG__)
 * Never prompts visitors or relies on per-device localStorage.
 */
export async function resolveFirebaseWebConfig({
  fetchImpl = globalThis.fetch,
  runtimeGlobal = typeof window !== "undefined" ? window.__STAR_ARTIST_FIREBASE_CONFIG__ : null
} = {}) {
  clearLegacyLocalWebConfig();

  // 1. Firebase Hosting free Spark auto-provisioned config endpoint
  const hostedCfg = await fetchJsonConfig("/__/firebase/init.json", {
    fetchImpl,
    cache: "no-store"
  });
  if (hostedCfg) {
    return {
      ...hostedCfg,
      configSource: "firebase-hosting"
    };
  }

  // 2. Centralized shared Firebase Web App config bundled with the deployed website
  const sharedCfg = await fetchJsonConfig("/firebase-config.json", {
    fetchImpl,
    cache: "no-cache"
  });
  if (sharedCfg) {
    return {
      ...sharedCfg,
      configSource: "shared-config"
    };
  }

  // 3. Optional runtime global injected by a custom deployment wrapper
  const globalCfg = sanitizeWebConfig(runtimeGlobal);
  if (globalCfg) {
    return {
      ...globalCfg,
      configSource: "runtime-global"
    };
  }

  return null;
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

async function loadFirebaseStorageModule() {
  if (!storageSdkPromise) {
    storageSdkPromise = import(`${SDK_BASE}/firebase-storage.js`);
  }
  return storageSdkPromise;
}

export async function initFirebaseClient() {
  const cfg = await resolveFirebaseWebConfig();
  if (!cfg) {
    return { ready: false, reason: "network_or_config_unavailable", defaults: FIREBASE_PROJECT_DEFAULTS };
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
    ...(await loadFirebaseModules())
  };
}

export async function subscribeAuthState(onChange) {
  const ctx = await initFirebaseClient();
  if (!ctx.ready) {
    onChange({ user: null, isAdmin: false, firebaseReady: false });
    return () => {};
  }

  // Complete any pending mobile redirect sign-in seamlessly
  if (typeof ctx.authMod.getRedirectResult === "function") {
    ctx.authMod.getRedirectResult(ctx.auth).catch(() => {});
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
  return isAuthorizedAdminUser(user);
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
  if (!ctx.ready) throw new Error("Unable to reach Star Artist Studio Cloud. Please check your internet connection.");
  const provider = new ctx.authMod.GoogleAuthProvider();
  provider.setCustomParameters({ prompt: "select_account" });
  try {
    return await ctx.authMod.signInWithPopup(ctx.auth, provider);
  } catch (err) {
    const code = String(err?.code || "");
    if (
      (code === "auth/popup-blocked" || code === "auth/operation-not-supported-in-this-environment") &&
      typeof ctx.authMod.signInWithRedirect === "function"
    ) {
      return ctx.authMod.signInWithRedirect(ctx.auth, provider);
    }
    throw err;
  }
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

  const title = String(payload.title || "").trim();
  const mediaUrl = String(payload.mediaUrl || "").trim();
  const rawThumb = String(payload.thumbnailUrl || "").trim();
  const thumbnailUrl = rawThumb || (mediaUrl.length <= 200000 ? mediaUrl : "");

  if (!title || title.length > 120) {
    throw new Error("Please enter an artwork title (1–120 characters).");
  }
  if (!mediaUrl || mediaUrl.length > 850000) {
    throw new Error("Please provide a valid image or compressed photo (within free cloud limit).");
  }
  if (thumbnailUrl.length > 200000) {
    throw new Error("Thumbnail exceeds the cloud size limit.");
  }

  const cleanMeta = payload.metadata && typeof payload.metadata === "object"
    ? {
        camera: String(payload.metadata.camera || "").trim().slice(0, 100),
        lens: String(payload.metadata.lens || "").trim().slice(0, 100),
        aperture: String(payload.metadata.aperture || "").trim().slice(0, 32),
        shutterSpeed: String(payload.metadata.shutterSpeed || "").trim().slice(0, 32),
        iso: String(payload.metadata.iso || "").trim().slice(0, 32),
        dateTaken: String(payload.metadata.dateTaken || "").trim().slice(0, 40)
      }
    : null;
  const hasMeta = Boolean(
    cleanMeta &&
      (cleanMeta.camera ||
        cleanMeta.lens ||
        cleanMeta.aperture ||
        cleanMeta.shutterSpeed ||
        cleanMeta.iso ||
        cleanMeta.dateTaken)
  );

  const { fsMod, db } = ctx;
  const docData = {
    userId: user.uid,
    title,
    category: payload.category || "Fashion",
    mediaType: payload.mediaType || "photo",
    mediaUrl,
    thumbnailUrl,
    isDemo: false,
    status: autoApprove ? "approved" : "pending",
    createdAt: fsMod.serverTimestamp(),
    updatedAt: fsMod.serverTimestamp()
  };
  if (hasMeta) {
    docData.metadata = cleanMeta;
  }

  const ref = await fsMod.addDoc(fsMod.collection(db, "gallery"), docData);
  return ref.id;
}

/**
 * Uploads an image file/blob to Firebase Storage using uploadBytesResumable so real-time
 * byte transfer snapshots update the dropzone progress bar live.
 */
export async function uploadGalleryImageToStorage(
  fileOrBlob,
  { originalName = "", onProgress = null, storageAdapter = null } = {}
) {
  if (!fileOrBlob) {
    throw new Error("No image file provided for Firebase Storage upload.");
  }

  let adapter = storageAdapter;
  let uid = "member";

  if (!adapter) {
    const ctx = await initFirebaseClient();
    if (!ctx.ready) {
      throw new Error("Firebase Web App is not configured.");
    }
    const user = ctx.auth.currentUser;
    if (!user) {
      throw new Error("Please sign in first to upload artwork to Firebase Storage.");
    }
    uid = user.uid;
    const storageMod = await loadFirebaseStorageModule();
    const storage = storageMod.getStorage(ctx.app);
    adapter = {
      ref: (path) => storageMod.ref(storage, path),
      uploadBytesResumable: (storageRef, data, metadata) =>
        storageMod.uploadBytesResumable(storageRef, data, metadata),
      getDownloadURL: (storageRef) => storageMod.getDownloadURL(storageRef)
    };
  } else if (adapter.uid) {
    uid = adapter.uid;
  }

  const fileName = originalName || fileOrBlob.name || "studio-photo.jpg";
  const storagePath = buildStorageObjectPath(uid, fileName);
  const storageRef = adapter.ref(storagePath);
  const contentType = fileOrBlob.type || "image/jpeg";
  const totalFallback = Number(fileOrBlob.size || 0);

  if (typeof onProgress === "function") {
    onProgress({
      percent: 0,
      bytesTransferred: 0,
      totalBytes: totalFallback,
      state: "running"
    });
  }

  const uploadTask = adapter.uploadBytesResumable(storageRef, fileOrBlob, { contentType });

  return new Promise((resolve, reject) => {
    uploadTask.on(
      "state_changed",
      (snapshot) => {
        const transferred = Number(snapshot?.bytesTransferred || 0);
        const total = Number(snapshot?.totalBytes || totalFallback || 0);
        const percent = computeUploadProgress(transferred, total);
        if (typeof onProgress === "function") {
          onProgress({
            percent,
            bytesTransferred: transferred,
            totalBytes: total,
            state: snapshot?.state || "running"
          });
        }
      },
      (error) => {
        reject(error);
      },
      async () => {
        try {
          const targetRef = uploadTask?.snapshot?.ref || storageRef;
          const downloadUrl = await adapter.getDownloadURL(targetRef);
          const finalTotal = Number(uploadTask?.snapshot?.totalBytes || totalFallback || 0);
          if (typeof onProgress === "function") {
            onProgress({
              percent: 100,
              bytesTransferred: finalTotal,
              totalBytes: finalTotal,
              state: "success"
            });
          }
          resolve({
            downloadUrl,
            storagePath,
            bytesTransferred: finalTotal,
            totalBytes: finalTotal
          });
        } catch (err) {
          reject(err);
        }
      }
    );
  });
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

// --- PUBLIC MODEL ROSTER & COMPANY / EVENT ORGANIZER BOOKINGS ---

export async function fetchPublicApprovedModels() {
  const ctx = await initFirebaseClient();
  if (!ctx.ready) return [];
  const { fsMod, db } = ctx;
  const snap = await fsMod.getDocs(fsMod.collection(db, "publicModels"));
  return snap.docs
    .map((d) => {
      const data = d.data() || {};
      const clean = sanitizePublicModelProfile({ ...data, id: d.id, modelId: data.modelId || d.id });
      return clean ? { ...clean, _sortTs: data.updatedAt?.seconds || data.createdAt?.seconds || 0 } : null;
    })
    .filter(Boolean)
    .sort((a, b) => (b._sortTs || 0) - (a._sortTs || 0))
    .map(({ _sortTs, ...clean }) => clean);
}

export async function fetchMyCompanyProfile(uid) {
  const ctx = await initFirebaseClient();
  if (!ctx.ready || !uid) return null;
  const { fsMod, db } = ctx;
  const snap = await fsMod.getDoc(fsMod.doc(db, "companies", uid));
  return snap.exists() ? { id: snap.id, ...snap.data() } : null;
}

export async function saveCompanyRegistration(payload) {
  const ctx = await initFirebaseClient();
  if (!ctx.ready) throw new Error("Firebase Web App is not configured.");
  const user = ctx.auth.currentUser;
  if (!user) throw new Error("Please sign in first to register your company or organizer profile.");

  const { fsMod, db } = ctx;
  const companyRef = fsMod.doc(db, "companies", user.uid);
  const existing = await fsMod.getDoc(companyRef);
  const baseFields = {
    companyName: String(payload.companyName || "").trim(),
    organizationType: String(payload.organizationType || "Company").trim(),
    contactPerson: String(payload.contactPerson || "").trim(),
    mobileNumber: String(payload.mobileNumber || "").trim(),
    email: String(payload.email || "").trim().toLowerCase(),
    address: String(payload.address || "").trim(),
    updatedAt: fsMod.serverTimestamp()
  };

  if (existing.exists()) {
    await fsMod.updateDoc(companyRef, baseFields);
  } else {
    await fsMod.setDoc(companyRef, {
      userId: user.uid,
      ...baseFields,
      createdAt: fsMod.serverTimestamp()
    });
  }
  return user.uid;
}

export async function fetchMyCompanyBookings(uid) {
  const ctx = await initFirebaseClient();
  if (!ctx.ready || !uid) return [];
  const { fsMod, db } = ctx;
  const q = fsMod.query(
    fsMod.collection(db, "companyBookings"),
    fsMod.where("userId", "==", uid)
  );
  const snap = await fsMod.getDocs(q);
  return snap.docs
    .map((d) => ({ id: d.id, ...d.data() }))
    .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
}

export async function submitCompanyModelBooking(payload) {
  const ctx = await initFirebaseClient();
  if (!ctx.ready) throw new Error("Firebase Web App is not configured.");
  const user = ctx.auth.currentUser;
  if (!user) throw new Error("Please sign in first to submit a model requirement booking.");

  const { fsMod, db } = ctx;
  const docData = {
    userId: user.uid,
    companyName: String(payload.companyName || "").trim(),
    organizationType: String(payload.organizationType || "Company").trim(),
    contactPerson: String(payload.contactPerson || "").trim(),
    mobileNumber: String(payload.mobileNumber || "").trim(),
    email: String(payload.email || "").trim().toLowerCase(),
    address: String(payload.address || "").trim(),
    modelRequirement: String(payload.modelRequirement || "").trim(),
    modelsCount: Number.parseInt(String(payload.modelsCount ?? "1"), 10),
    eventType: String(payload.eventType || "").trim(),
    eventDate: String(payload.eventDate || "").trim(),
    eventLocation: String(payload.eventLocation || "").trim(),
    budget: String(payload.budget || "").trim(),
    detailedRequirements: String(payload.detailedRequirements || "").trim(),
    requestedModelIds: Array.isArray(payload.requestedModelIds) ? payload.requestedModelIds.slice(0, 20) : [],
    requestedModelNames: Array.isArray(payload.requestedModelNames) ? payload.requestedModelNames.slice(0, 20) : [],
    status: "Pending",
    adminNotes: "",
    createdAt: fsMod.serverTimestamp(),
    updatedAt: fsMod.serverTimestamp()
  };

  const ref = await fsMod.addDoc(fsMod.collection(db, "companyBookings"), docData);
  // Keep registered company profile in sync best-effort
  saveCompanyRegistration(payload).catch(() => {});
  return ref.id;
}

export async function cancelMyCompanyBooking(requestId) {
  const ctx = await initFirebaseClient();
  if (!ctx.ready || !ctx.auth.currentUser) throw new Error("Sign in required.");
  const { fsMod, db } = ctx;
  await fsMod.updateDoc(fsMod.doc(db, "companyBookings", requestId), {
    status: "Cancelled",
    updatedAt: fsMod.serverTimestamp()
  });
}

// --- ADMIN-ONLY OPERATIONS (Enforced both client-side and server-side by firestore.rules) ---

function requireAuthorizedAdmin(ctx) {
  if (!ctx?.ready || !ctx?.auth?.currentUser) {
    throw new Error("Admin sign-in required.");
  }
  if (!isAuthorizedAdminUser(ctx.auth.currentUser)) {
    throw new Error("Access denied: authorized Studio Admin Gmail account required.");
  }
  return ctx.auth.currentUser;
}

export async function adminFetchDashboardData() {
  const ctx = await initFirebaseClient();
  requireAuthorizedAdmin(ctx);
  const { fsMod, db } = ctx;
  const [bookingsSnap, modelsSnap, gallerySnap, announcementsSnap, companiesSnap, companyBookingsSnap] = await Promise.all([
    fsMod.getDocs(fsMod.collection(db, "bookings")),
    fsMod.getDocs(fsMod.collection(db, "models")),
    fsMod.getDocs(fsMod.collection(db, "gallery")),
    fsMod.getDocs(fsMod.collection(db, "announcements")),
    fsMod.getDocs(fsMod.collection(db, "companies")),
    fsMod.getDocs(fsMod.collection(db, "companyBookings"))
  ]);
  const mapSorted = (snap) =>
    snap.docs
      .map((d) => ({ id: d.id, ...d.data() }))
      .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
  return {
    bookings: mapSorted(bookingsSnap),
    models: mapSorted(modelsSnap),
    gallery: mapSorted(gallerySnap),
    announcements: mapSorted(announcementsSnap),
    companies: mapSorted(companiesSnap),
    companyBookings: mapSorted(companyBookingsSnap)
  };
}

export async function adminUpdateBookingStatus(bookingId, status) {
  const ctx = await initFirebaseClient();
  requireAuthorizedAdmin(ctx);
  const { fsMod, db } = ctx;
  await fsMod.updateDoc(fsMod.doc(db, "bookings", bookingId), {
    status,
    updatedAt: fsMod.serverTimestamp()
  });
}

export async function adminAutoCompleteExpiredBookings(bookingIds = []) {
  const ctx = await initFirebaseClient();
  requireAuthorizedAdmin(ctx);
  const ids = [...new Set((Array.isArray(bookingIds) ? bookingIds : []).filter(Boolean))];
  if (!ids.length) {
    return { succeededIds: [], failedIds: [] };
  }
  const { fsMod, db } = ctx;
  const results = await Promise.allSettled(
    ids.map((id) =>
      fsMod.updateDoc(fsMod.doc(db, "bookings", id), {
        status: "Completed",
        updatedAt: fsMod.serverTimestamp()
      })
    )
  );
  const succeededIds = [];
  const failedIds = [];
  results.forEach((res, idx) => {
    if (res.status === "fulfilled") succeededIds.push(ids[idx]);
    else failedIds.push(ids[idx]);
  });
  return { succeededIds, failedIds };
}

export async function adminSubscribeBookingsRealtime({ onBookings, onError } = {}) {
  const ctx = await initFirebaseClient();
  requireAuthorizedAdmin(ctx);
  const { fsMod, db } = ctx;
  return fsMod.onSnapshot(
    fsMod.collection(db, "bookings"),
    (snap) => {
      const bookings = snap.docs
        .map((d) => ({ id: d.id, ...d.data() }))
        .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
      if (typeof onBookings === "function") {
        onBookings(bookings);
      }
    },
    (err) => {
      if (typeof onError === "function") {
        onError(err);
      }
    }
  );
}

export async function adminDeleteBooking(bookingId) {
  const ctx = await initFirebaseClient();
  requireAuthorizedAdmin(ctx);
  const { fsMod, db } = ctx;
  await fsMod.deleteDoc(fsMod.doc(db, "bookings", bookingId));
}

export async function adminUpdateModelStatus(modelId, status, adminNotes = "", modelData = null) {
  const ctx = await initFirebaseClient();
  requireAuthorizedAdmin(ctx);
  const { fsMod, db } = ctx;
  await fsMod.updateDoc(fsMod.doc(db, "models", modelId), {
    status,
    adminNotes: adminNotes.trim(),
    updatedAt: fsMod.serverTimestamp()
  });

  // Sync privacy-safe public portfolio card (strictly excluding phoneNumber/email/userId/adminNotes)
  const publicRef = fsMod.doc(db, "publicModels", modelId);
  if (status === "approved" && modelData) {
    const clean = sanitizePublicModelProfile({
      ...modelData,
      id: modelId,
      modelId
    });
    if (clean) {
      await fsMod.setDoc(publicRef, {
        modelId: clean.modelId,
        fullName: clean.fullName,
        gender: clean.gender,
        city: clean.city,
        skills: clean.skills,
        portfolioUrl: clean.portfolioUrl,
        avatarUrl: clean.avatarUrl,
        category: clean.category,
        isOfficialRoster: true,
        createdAt: fsMod.serverTimestamp(),
        updatedAt: fsMod.serverTimestamp()
      });
    }
  } else if (status !== "approved") {
    try {
      await fsMod.deleteDoc(publicRef);
    } catch {
      // Public showcase card may not exist yet
    }
  }
}

export async function adminUpdateCompanyBookingStatus(requestId, status, adminNotes = "") {
  const ctx = await initFirebaseClient();
  requireAuthorizedAdmin(ctx);
  const { fsMod, db } = ctx;
  await fsMod.updateDoc(fsMod.doc(db, "companyBookings", requestId), {
    status,
    adminNotes: String(adminNotes || "").trim(),
    updatedAt: fsMod.serverTimestamp()
  });
}

export async function adminDeleteCompanyBooking(requestId) {
  const ctx = await initFirebaseClient();
  requireAuthorizedAdmin(ctx);
  const { fsMod, db } = ctx;
  await fsMod.deleteDoc(fsMod.doc(db, "companyBookings", requestId));
}

export async function adminUpdateGalleryStatus(itemId, status) {
  const ctx = await initFirebaseClient();
  requireAuthorizedAdmin(ctx);
  const { fsMod, db } = ctx;
  await fsMod.updateDoc(fsMod.doc(db, "gallery", itemId), {
    status,
    updatedAt: fsMod.serverTimestamp()
  });
}

export async function adminDeleteGalleryItem(itemId) {
  const ctx = await initFirebaseClient();
  requireAuthorizedAdmin(ctx);
  const { fsMod, db } = ctx;
  await fsMod.deleteDoc(fsMod.doc(db, "gallery", itemId));
}

export async function adminPublishAnnouncement({ title, message, category, priority }) {
  const ctx = await initFirebaseClient();
  const user = requireAuthorizedAdmin(ctx);
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
