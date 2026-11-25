import { FIREBASE_PROJECT_DEFAULTS, STUDIO_CONTACT } from "./data.js";
import { buildStorageObjectPath, computeUploadProgress } from "./image-upload.js";
import { isAuthorizedAdminUser } from "./admin-guard.js";
import { sanitizePublicModelProfile } from "./company-booking.js";
import {
  sanitizePortfolioPhotoList,
  buildModelNotificationPayload,
  validateModelSelfRegistration
} from "./model-portal.js";
import { validateWeddingTierConfig, WEDDING_TIER_IDS } from "./wedding-packages.js";

const LEGACY_STORAGE_CONFIG_KEY = "star_artist_firebase_web_config_v1";
const LOCAL_DEV_SESSION_CONFIG_KEY = "star_artist_local_dev_firebase_config_v1";
const FIREBASE_SDK_VERSION = "11.4.0";
const SDK_BASE = `https://www.gstatic.com/firebasejs/${FIREBASE_SDK_VERSION}`;
const CLOUD_UNAVAILABLE_MESSAGE =
  "Unable to reach Star Artist Studio Cloud right now. Please check your internet connection and try again.";

let sdkPromise = null;
let storageSdkPromise = null;
let appInstance = null;
let authInstance = null;
let dbInstance = null;
let activeConfig = null;

function getCurrentHostname(hostnameOverride) {
  if (typeof hostnameOverride === "string") {
    return hostnameOverride.trim().toLowerCase();
  }
  if (typeof window !== "undefined" && window.location && typeof window.location.hostname === "string") {
    return window.location.hostname.trim().toLowerCase();
  }
  return "";
}

/**
 * Returns true on Firebase Hosting production domains (*.web.app and *.firebaseapp.com).
 */
export function isFirebaseHostingProductionHost(hostname) {
  const host = getCurrentHostname(hostname);
  if (!host) return false;
  return host === "web.app" || host.endsWith(".web.app") || host === "firebaseapp.com" || host.endsWith(".firebaseapp.com");
}

/**
 * Returns true strictly for local developer preview environments.
 */
export function isLocalDevelopmentHost(hostname) {
  const host = getCurrentHostname(hostname);
  if (!host) return false;
  return (
    host === "localhost" ||
    host === "127.0.0.1" ||
    host === "0.0.0.0" ||
    host === "::1" ||
    host === "[::1]" ||
    host.endsWith(".local")
  );
}

/**
 * Manual Firebase setup is NEVER permitted on production Firebase Hosting (.web.app / .firebaseapp.com)
 * or any visitor/mobile domain. It may only appear in local development preview when auto-config is unavailable.
 */
export function shouldShowManualFirebaseSetup({ hostname, firebaseReady = false } = {}) {
  if (firebaseReady) return false;
  if (isFirebaseHostingProductionHost(hostname)) return false;
  return isLocalDevelopmentHost(hostname);
}

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

function readLocalDevSessionConfig(hostname) {
  if (!isLocalDevelopmentHost(hostname)) return null;
  try {
    if (typeof sessionStorage === "undefined") return null;
    const raw = sessionStorage.getItem(LOCAL_DEV_SESSION_CONFIG_KEY);
    if (!raw) return null;
    return sanitizeWebConfig(JSON.parse(raw));
  } catch {
    return null;
  }
}

export function saveLocalDevFirebaseConfig(rawConfig, { hostname } = {}) {
  if (!isLocalDevelopmentHost(hostname)) {
    return null;
  }
  const clean = sanitizeWebConfig(rawConfig);
  if (!clean) {
    throw new Error("Please enter a valid Firebase Web API Key and Web App ID for local preview.");
  }
  try {
    if (typeof sessionStorage !== "undefined") {
      sessionStorage.setItem(LOCAL_DEV_SESSION_CONFIG_KEY, JSON.stringify(clean));
    }
  } catch {
    // Ignore restricted storage contexts
  }
  return clean;
}

export function clearLocalDevFirebaseConfig() {
  try {
    if (typeof sessionStorage !== "undefined") {
      sessionStorage.removeItem(LOCAL_DEV_SESSION_CONFIG_KEY);
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
 * 1. On Firebase Hosting production domains (.web.app / .firebaseapp.com), automatically loads
 *    /__/firebase/init.json (with a single network retry on flaky mobile links).
 * 2. Falls back to the centralized shared web configuration (/firebase-config.json) for offline PWA
 *    launches, custom domains, or preview servers.
 * 3. Optional runtime global override (window.__STAR_ARTIST_FIREBASE_CONFIG__).
 * 4. Strictly on localhost/127.0.0.1 development previews only, supports a sessionStorage override if needed.
 * Never prompts production visitors or relies on per-device localStorage.
 */
export async function resolveFirebaseWebConfig({
  fetchImpl = globalThis.fetch,
  hostname,
  runtimeGlobal = typeof window !== "undefined" ? window.__STAR_ARTIST_FIREBASE_CONFIG__ : null
} = {}) {
  const host = getCurrentHostname(hostname);
  const isProdHosting = isFirebaseHostingProductionHost(host);

  clearLegacyLocalWebConfig();
  if (!isLocalDevelopmentHost(host)) {
    clearLocalDevFirebaseConfig();
  }

  // 1. Firebase Hosting free Spark auto-provisioned config endpoint
  let hostedCfg = await fetchJsonConfig("/__/firebase/init.json", {
    fetchImpl,
    cache: "no-store"
  });
  if (!hostedCfg && isProdHosting) {
    // Single resilient retry on production Firebase Hosting in case of a brief mobile network blip
    hostedCfg = await fetchJsonConfig("/__/firebase/init.json", {
      fetchImpl,
      cache: "no-store"
    });
  }
  if (hostedCfg) {
    return {
      ...hostedCfg,
      configSource: "firebase-hosting"
    };
  }

  // 2. Centralized shared Firebase Web App config bundled with the deployed website (also offline PWA fallback)
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

  // 4. Local developer preview fallback ONLY (never read on production .web.app / .firebaseapp.com)
  const localDevCfg = readLocalDevSessionConfig(host);
  if (localDevCfg) {
    return {
      ...localDevCfg,
      configSource: "local-dev-session"
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

const GOOGLE_AUTH_MODE_STORAGE_KEY = "star_artist_google_auth_mode";
let authEnforcementInProgress = false;
let activeAuthStateCallback = null;

function readPendingGoogleAuthMode() {
  try {
    if (typeof sessionStorage === "undefined") return "login";
    const mode = sessionStorage.getItem(GOOGLE_AUTH_MODE_STORAGE_KEY);
    sessionStorage.removeItem(GOOGLE_AUTH_MODE_STORAGE_KEY);
    return mode === "signup" ? "signup" : "login";
  } catch {
    return "login";
  }
}

function savePendingGoogleAuthMode(mode) {
  try {
    if (typeof sessionStorage !== "undefined") {
      sessionStorage.setItem(GOOGLE_AUTH_MODE_STORAGE_KEY, mode === "signup" ? "signup" : "login");
    }
  } catch {
    // Ignore restricted storage contexts
  }
}

function createUnregisteredAccountError() {
  const err = new Error(
    "This account is not registered yet. Please create an account first on the Create Account / Sign Up page."
  );
  err.code = "auth/unregistered-account";
  return err;
}

async function emitVerifiedAuthState(user) {
  if (typeof activeAuthStateCallback !== "function") return;
  if (!user || user.isAnonymous === true) {
    activeAuthStateCallback({ user: null, isAdmin: false, firebaseReady: true });
    return;
  }
  const admin = await checkIsAdmin(user);
  await ensureUserDocument(user);
  activeAuthStateCallback({ user, isAdmin: admin, firebaseReady: true });
}

async function enforceGoogleRegistrationPolicy(ctx, cred, mode = "login") {
  if (!cred || !cred.user) return { cred, alreadyRegistered: false };
  const info =
    typeof ctx.authMod.getAdditionalUserInfo === "function"
      ? ctx.authMod.getAdditionalUserInfo(cred)
      : null;
  const isNewUser = Boolean(info?.isNewUser);
  const isAdminAccount = isAuthorizedAdminUser(cred.user);

  if (mode === "login" && isNewUser && !isAdminAccount) {
    const prevEnforcement = authEnforcementInProgress;
    authEnforcementInProgress = true;
    try {
      if (typeof cred.user.delete === "function") {
        await cred.user.delete().catch(() => {});
      }
      await ctx.authMod.signOut(ctx.auth).catch(() => {});
    } finally {
      authEnforcementInProgress = prevEnforcement;
    }
    throw createUnregisteredAccountError();
  }

  await ensureUserDocument(cred.user);
  return { cred, alreadyRegistered: !isNewUser };
}

export async function subscribeAuthState(onChange) {
  activeAuthStateCallback = typeof onChange === "function" ? onChange : null;
  const ctx = await initFirebaseClient();
  if (!ctx.ready) {
    onChange({ user: null, isAdmin: false, firebaseReady: false });
    return () => {};
  }

  // Complete any pending mobile redirect sign-in while enforcing mandatory registration
  if (typeof ctx.authMod.getRedirectResult === "function") {
    const redirectMode = readPendingGoogleAuthMode();
    try {
      const redirectCred = await ctx.authMod.getRedirectResult(ctx.auth);
      if (redirectCred && redirectCred.user) {
        await enforceGoogleRegistrationPolicy(ctx, redirectCred, redirectMode);
      }
    } catch (err) {
      if (err?.code === "auth/unregistered-account") {
        onChange({
          user: null,
          isAdmin: false,
          firebaseReady: true,
          authError: err
        });
      }
    }
  }

  return ctx.authMod.onAuthStateChanged(ctx.auth, async (user) => {
    if (authEnforcementInProgress) {
      return;
    }
    if (!user || user.isAnonymous === true) {
      if (user?.isAnonymous === true) {
        await ctx.authMod.signOut(ctx.auth).catch(() => {});
      }
      onChange({ user: null, isAdmin: false, firebaseReady: true });
      return;
    }
    await emitVerifiedAuthState(user);
  });
}

export async function checkIsAdmin(user) {
  return isAuthorizedAdminUser(user);
}

async function ensureUserDocument(user, { fullName = "" } = {}) {
  const ctx = await initFirebaseClient();
  if (!ctx.ready || !user || user.isAnonymous === true) return;
  const { fsMod, db } = ctx;
  const userRef = fsMod.doc(db, "users", user.uid);
  const resolvedName = String(fullName || user.displayName || "Star Artist Member").trim() || "Star Artist Member";
  try {
    const existing = await fsMod.getDoc(userRef);
    if (!existing.exists()) {
      await fsMod.setDoc(userRef, {
        userId: user.uid,
        email: user.email || "",
        displayName: resolvedName,
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

export async function signInWithGooglePopup({ mode = "login" } = {}) {
  const ctx = await initFirebaseClient();
  if (!ctx.ready) throw new Error("Unable to reach Star Artist Studio Cloud. Please check your internet connection.");
  const normalizedMode = mode === "signup" ? "signup" : "login";
  const provider = new ctx.authMod.GoogleAuthProvider();
  provider.setCustomParameters({ prompt: "select_account" });
  authEnforcementInProgress = true;
  try {
    const cred = await ctx.authMod.signInWithPopup(ctx.auth, provider);
    const result = await enforceGoogleRegistrationPolicy(ctx, cred, normalizedMode);
    authEnforcementInProgress = false;
    await emitVerifiedAuthState(cred.user);
    return result;
  } catch (err) {
    authEnforcementInProgress = false;
    const code = String(err?.code || "");
    if (
      (code === "auth/popup-blocked" || code === "auth/operation-not-supported-in-this-environment") &&
      typeof ctx.authMod.signInWithRedirect === "function"
    ) {
      savePendingGoogleAuthMode(normalizedMode);
      return ctx.authMod.signInWithRedirect(ctx.auth, provider);
    }
    throw err;
  }
}

export async function signInWithEmailPassword(email, password) {
  const ctx = await initFirebaseClient();
  if (!ctx.ready) throw new Error(CLOUD_UNAVAILABLE_MESSAGE);
  const cred = await ctx.authMod.signInWithEmailAndPassword(ctx.auth, email.trim(), password);
  await ensureUserDocument(cred.user);
  return cred;
}

export async function registerWithEmailPassword(email, password, { fullName = "" } = {}) {
  const ctx = await initFirebaseClient();
  if (!ctx.ready) throw new Error(CLOUD_UNAVAILABLE_MESSAGE);
  const cleanName = String(fullName || "").trim();
  const cred = await ctx.authMod.createUserWithEmailAndPassword(ctx.auth, email.trim(), password);
  if (cleanName && typeof ctx.authMod.updateProfile === "function") {
    try {
      await ctx.authMod.updateProfile(cred.user, { displayName: cleanName });
    } catch {
      // Optional display name update fallback
    }
  }
  await ensureUserDocument(cred.user, { fullName: cleanName });
  try {
    await ctx.authMod.sendEmailVerification(cred.user);
  } catch {
    // Verification email optional fallback
  }
  return cred;
}

function hasVerifiedMemberAuth(ctx) {
  const u = ctx?.auth?.currentUser;
  return Boolean(ctx?.ready && u && u.isAnonymous !== true && String(u.uid || "").trim().length > 0);
}

export async function sendPasswordReset(email) {
  const ctx = await initFirebaseClient();
  if (!ctx.ready) throw new Error(CLOUD_UNAVAILABLE_MESSAGE);
  const cleanEmail = String(email || "").trim().toLowerCase();
  await ctx.authMod.sendPasswordResetEmail(ctx.auth, cleanEmail);
}

export async function signOutFirebase() {
  const ctx = await initFirebaseClient();
  if (!ctx.ready) return;
  await ctx.authMod.signOut(ctx.auth);
}

export async function fetchApprovedGallery() {
  const ctx = await initFirebaseClient();
  if (!hasVerifiedMemberAuth(ctx)) return [];
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
  if (!ctx.ready) throw new Error(CLOUD_UNAVAILABLE_MESSAGE);
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
  if (!ctx.ready) throw new Error(CLOUD_UNAVAILABLE_MESSAGE);
  const user = ctx.auth.currentUser;
  if (!user) throw new Error("Please sign in first to apply as an 18+ model.");

  const check = validateModelSelfRegistration(payload, { isEdit: false });
  if (!check.valid) {
    throw new Error(check.errors[0] || "Please check all required registration fields.");
  }

  const c = check.cleaned;
  const { fsMod, db } = ctx;
  const docPayload = {
    userId: user.uid,
    fullName: c.fullName,
    age: c.age,
    gender: c.gender,
    city: c.city,
    phoneNumber: c.phoneNumber,
    skills: c.skills,
    portfolioUrls: c.portfolioUrls,
    photoConsent: c.photoConsent,
    isDisabled: false,
    consentGiven: true,
    status: "pending",
    adminNotes: "",
    createdAt: fsMod.serverTimestamp(),
    updatedAt: fsMod.serverTimestamp()
  };

  if (c.dateOfBirth) docPayload.dateOfBirth = c.dateOfBirth;
  if (c.state) docPayload.state = c.state;
  if (Number.isInteger(c.heightCm)) docPayload.heightCm = c.heightCm;
  if (c.languages) docPayload.languages = c.languages;
  if (c.experienceLevel) docPayload.experienceLevel = c.experienceLevel;
  if (c.bio) docPayload.bio = c.bio;
  if (c.contactEmail) docPayload.contactEmail = c.contactEmail;
  if (c.introVideoUrl) docPayload.introVideoUrl = c.introVideoUrl;
  if (c.portfolioPhotos.length > 0) docPayload.portfolioPhotos = c.portfolioPhotos;

  const ref = await fsMod.addDoc(fsMod.collection(db, "models"), docPayload);
  return ref.id;
}

export async function updateMyModelProfile(modelId, payload) {
  const ctx = await initFirebaseClient();
  if (!ctx.ready) throw new Error(CLOUD_UNAVAILABLE_MESSAGE);
  const user = ctx.auth.currentUser;
  if (!user) throw new Error("Please sign in first to edit your model profile.");

  const check = validateModelSelfRegistration(payload, { isEdit: true });
  if (!check.valid) {
    throw new Error(check.errors[0] || "Please check your profile details.");
  }

  const c = check.cleaned;
  const { fsMod, db } = ctx;
  const updatePayload = {
    fullName: c.fullName,
    gender: c.gender,
    dateOfBirth: c.dateOfBirth,
    age: c.age,
    city: c.city,
    state: c.state,
    skills: c.skills,
    languages: c.languages,
    experienceLevel: c.experienceLevel,
    bio: c.bio,
    portfolioUrls: c.portfolioUrls,
    portfolioPhotos: sanitizePortfolioPhotoList(c.portfolioPhotos),
    introVideoUrl: c.introVideoUrl,
    contactEmail: c.contactEmail,
    phoneNumber: c.phoneNumber,
    photoConsent: c.photoConsent,
    updatedAt: fsMod.serverTimestamp()
  };
  if (Number.isInteger(c.heightCm)) {
    updatePayload.heightCm = c.heightCm;
  }

  await fsMod.updateDoc(fsMod.doc(db, "models", modelId), updatePayload);
}

export async function fetchMyNotifications(uid) {
  const ctx = await initFirebaseClient();
  if (!ctx.ready || !uid) return [];
  const { fsMod, db } = ctx;
  try {
    const snap = await fsMod.getDocs(fsMod.collection(db, "users", uid, "notifications"));
    return snap.docs
      .map((d) => ({ id: d.id, ...d.data() }))
      .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
  } catch {
    return [];
  }
}

export async function markMyNotificationRead(uid, notificationId) {
  const ctx = await initFirebaseClient();
  if (!ctx.ready || !uid || !notificationId) return;
  const { fsMod, db } = ctx;
  await fsMod.updateDoc(fsMod.doc(db, "users", uid, "notifications", notificationId), {
    isRead: true,
    updatedAt: fsMod.serverTimestamp()
  });
}

export async function submitGalleryUpload(payload, autoApprove = false) {
  const ctx = await initFirebaseClient();
  if (!ctx.ready) throw new Error(CLOUD_UNAVAILABLE_MESSAGE);
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
      throw new Error(CLOUD_UNAVAILABLE_MESSAGE);
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
  if (!hasVerifiedMemberAuth(ctx)) return [];
  const { fsMod, db } = ctx;
  const snap = await fsMod.getDocs(fsMod.collection(db, "reviews"));
  return snap.docs
    .map((d) => ({ id: d.id, ...d.data() }))
    .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
}

export async function submitCatalogReview({ packageId, serviceType, rating, comment }) {
  const ctx = await initFirebaseClient();
  if (!ctx.ready) throw new Error(CLOUD_UNAVAILABLE_MESSAGE);
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
  if (!hasVerifiedMemberAuth(ctx)) return [];
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
  if (!ctx.ready) throw new Error(CLOUD_UNAVAILABLE_MESSAGE);
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
  if (!ctx.ready) throw new Error(CLOUD_UNAVAILABLE_MESSAGE);
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

async function writeModelStatusNotification(ctx, { modelId, modelData, status, adminNotes, isDisabled }) {
  const notif = buildModelNotificationPayload({
    modelId,
    userId: modelData?.userId,
    fullName: modelData?.fullName,
    status,
    adminNotes,
    isDisabled
  });
  if (!notif) return;
  try {
    const { fsMod, db } = ctx;
    const notifId = `model_${modelId}_${Date.now()}`.replace(/[^a-zA-Z0-9_-]/g, "_").slice(0, 120);
    await fsMod.setDoc(fsMod.doc(db, "users", notif.userId, "notifications", notifId), {
      ...notif,
      createdAt: fsMod.serverTimestamp(),
      updatedAt: fsMod.serverTimestamp()
    });
  } catch (err) {
    console.warn("Model notification dispatch skipped:", err?.message || err);
  }
}

async function syncPublicModelCatalogDoc(ctx, modelId, { status, isDisabled = false, modelData = null } = {}) {
  const { fsMod, db } = ctx;
  const publicRef = fsMod.doc(db, "publicModels", modelId);
  if (status === "approved" && !isDisabled && modelData) {
    const clean = sanitizePublicModelProfile({
      ...modelData,
      id: modelId,
      modelId,
      status: "approved",
      isOfficialRoster: true,
      isDisabled: false
    });
    if (clean) {
      const publicDoc = {
        modelId: clean.modelId,
        fullName: clean.fullName,
        gender: clean.gender,
        city: clean.city,
        state: clean.state,
        languages: clean.languages,
        experienceLevel: clean.experienceLevel,
        skills: clean.skills,
        bio: clean.bio,
        portfolioUrl: clean.portfolioUrl,
        portfolioPhotos: clean.photoConsent ? clean.portfolioPhotos : [],
        introVideoUrl: clean.introVideoUrl,
        portfolioAvailable: Boolean(clean.hasPortfolio),
        photoConsent: Boolean(clean.photoConsent),
        avatarUrl: clean.photoConsent && clean.portfolioPhotos.length === 0 ? clean.avatarUrl : "",
        category: clean.category,
        isOfficialRoster: true,
        createdAt: fsMod.serverTimestamp(),
        updatedAt: fsMod.serverTimestamp()
      };
      if (Number.isInteger(clean.age) && clean.age >= 18 && clean.age <= 100) {
        publicDoc.age = clean.age;
      }
      if (Number.isInteger(clean.heightCm) && clean.heightCm >= 120 && clean.heightCm <= 230) {
        publicDoc.heightCm = clean.heightCm;
      }
      await fsMod.setDoc(publicRef, publicDoc);
    }
  } else {
    try {
      await fsMod.deleteDoc(publicRef);
    } catch {
      // Public showcase card may not exist yet
    }
  }
}

export async function adminUpdateModelStatus(modelId, status, adminNotes = "", modelData = null) {
  const ctx = await initFirebaseClient();
  requireAuthorizedAdmin(ctx);
  const { fsMod, db } = ctx;
  const nextDisabled = status === "approved" ? false : Boolean(modelData?.isDisabled);
  await fsMod.updateDoc(fsMod.doc(db, "models", modelId), {
    status,
    isDisabled: nextDisabled,
    adminNotes: String(adminNotes || "").trim(),
    updatedAt: fsMod.serverTimestamp()
  });

  // Sync privacy-safe public portfolio card (strictly excluding phoneNumber/email/dateOfBirth/userId/adminNotes)
  await syncPublicModelCatalogDoc(ctx, modelId, { status, isDisabled: nextDisabled, modelData });
  await writeModelStatusNotification(ctx, {
    modelId,
    modelData,
    status,
    adminNotes,
    isDisabled: nextDisabled
  });
}

export async function adminSetModelDisabled(modelId, isDisabled, adminNotes = "", modelData = null) {
  const ctx = await initFirebaseClient();
  requireAuthorizedAdmin(ctx);
  const { fsMod, db } = ctx;
  const disabledFlag = Boolean(isDisabled);
  const currentStatus = String(modelData?.status || "pending").toLowerCase();
  await fsMod.updateDoc(fsMod.doc(db, "models", modelId), {
    isDisabled: disabledFlag,
    adminNotes: String(adminNotes || "").trim(),
    updatedAt: fsMod.serverTimestamp()
  });
  await syncPublicModelCatalogDoc(ctx, modelId, {
    status: currentStatus,
    isDisabled: disabledFlag,
    modelData
  });
  await writeModelStatusNotification(ctx, {
    modelId,
    modelData,
    status: currentStatus,
    adminNotes,
    isDisabled: disabledFlag
  });
}

export async function adminDeleteModelProfile(modelId) {
  const ctx = await initFirebaseClient();
  requireAuthorizedAdmin(ctx);
  const { fsMod, db } = ctx;
  try {
    await fsMod.deleteDoc(fsMod.doc(db, "publicModels", modelId));
  } catch {
    // Public catalog entry may not exist
  }
  await fsMod.deleteDoc(fsMod.doc(db, "models", modelId));
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

export async function adminRequestTestEmail({
  templateType = "booking",
  recordId = "",
  recordSnapshot = {},
  isSample = true,
  templateOverrides = {},
  subject = ""
} = {}) {
  const ctx = await initFirebaseClient();
  const user = requireAuthorizedAdmin(ctx);
  const { fsMod, db } = ctx;
  const safeType = ["booking", "model", "company"].includes(templateType) ? templateType : "booking";
  const recipientEmail = String(STUDIO_CONTACT.adminEmail || "chanderstudio0@gmail.com").trim().toLowerCase();
  const safeSubject = String(subject || `[TEST] ${safeType} notification`).trim().slice(0, 200);
  const testId = `emailtest_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;

  await fsMod.setDoc(fsMod.doc(db, "adminEmailTests", testId), {
    requestedByUid: user.uid,
    requestedByEmail: String(user.email || recipientEmail).trim().toLowerCase(),
    recipientEmail,
    templateType: safeType,
    recordId: String(recordId || "").trim().slice(0, 128),
    isSample: Boolean(isSample),
    recordSnapshot: recordSnapshot && typeof recordSnapshot === "object" ? recordSnapshot : {},
    templateOverrides: templateOverrides && typeof templateOverrides === "object" ? templateOverrides : {},
    subject: safeSubject,
    status: "queued",
    createdAt: fsMod.serverTimestamp()
  });

  return { testId, recipientEmail, subject: safeSubject };
}

export async function adminSubscribeTestEmailStatus(testId, { onUpdate, onError } = {}) {
  const ctx = await initFirebaseClient();
  requireAuthorizedAdmin(ctx);
  if (!testId) return () => {};
  const { fsMod, db } = ctx;
  return fsMod.onSnapshot(
    fsMod.doc(db, "adminEmailTests", testId),
    (snap) => {
      if (!snap.exists()) return;
      if (typeof onUpdate === "function") {
        onUpdate({ id: snap.id, ...snap.data() });
      }
    },
    (err) => {
      if (typeof onError === "function") {
        onError(err);
      }
    }
  );
}

// --- CONFIGURABLE WEDDING PACKAGE TIERS (Public read, Admin-only write on Spark Free Plan) ---

export async function fetchWeddingPackageConfigs() {
  const ctx = await initFirebaseClient();
  if (!hasVerifiedMemberAuth(ctx)) return [];
  const { fsMod, db } = ctx;
  try {
    const snap = await fsMod.getDocs(fsMod.collection(db, "servicePackages"));
    return snap.docs
      .map((d) => ({ id: d.id, ...d.data() }))
      .filter((doc) => WEDDING_TIER_IDS.includes(String(doc.tierId || doc.id || "").trim()));
  } catch {
    return [];
  }
}

export async function adminSaveWeddingPackageConfig(tierId, payload) {
  const ctx = await initFirebaseClient();
  const user = requireAuthorizedAdmin(ctx);
  const check = validateWeddingTierConfig(tierId, payload);
  if (!check.valid) {
    throw new Error(check.errors[0] || "Invalid wedding package configuration.");
  }
  const c = check.cleaned;
  const { fsMod, db } = ctx;
  const docData = {
    tierId: c.tierId,
    tierNameEn: c.tierNameEn,
    tierNameHi: c.tierNameHi,
    priceInr: c.priceInr,
    durationEn: c.durationEn,
    turnaroundEn: c.turnaroundEn,
    includedServices: c.includedServices,
    updatedByEmail: String(user.email || STUDIO_CONTACT.adminEmail).trim().toLowerCase(),
    updatedAt: fsMod.serverTimestamp()
  };
  await fsMod.setDoc(fsMod.doc(db, "servicePackages", c.tierId), docData);
  return { id: c.tierId, ...c };
}

export async function adminResetWeddingPackageConfig(tierId) {
  const ctx = await initFirebaseClient();
  requireAuthorizedAdmin(ctx);
  const cleanId = String(tierId || "").trim();
  if (!WEDDING_TIER_IDS.includes(cleanId)) {
    throw new Error("Invalid wedding package tier.");
  }
  const { fsMod, db } = ctx;
  try {
    await fsMod.deleteDoc(fsMod.doc(db, "servicePackages", cleanId));
  } catch {
    // Default tier already active if no override doc exists
  }
  return cleanId;
}

