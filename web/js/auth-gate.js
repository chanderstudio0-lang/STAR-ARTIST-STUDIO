import { isAuthorizedAdminUser, resolveSafeRoute, extractRequestedRoute, isAdminUrlAttempt } from "./admin-guard.js";

export const AUTH_ROUTES = ["login", "signup", "reset"];
export const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * Returns true ONLY for a real, non-anonymous Firebase authenticated account.
 * Explicitly blocks guest / anonymous sessions.
 */
export function isAuthenticatedMember(user) {
  if (!user || typeof user !== "object") return false;
  if (user.isAnonymous === true) return false;
  const uid = typeof user.uid === "string" ? user.uid.trim() : "";
  return uid.length > 0;
}

export function isAuthRoute(route) {
  const clean = String(route || "").trim().toLowerCase();
  return AUTH_ROUTES.includes(clean);
}

/**
 * Detects explicit Login, Create Account / Sign Up, or Password Reset URL targets
 * (#login, #signup, #register, #reset, #forgot-password, /login, /signup, /reset, ?route=login|signup|reset).
 */
export function extractAuthRouteFromUrl({ pathname = "", hash = "", search = "" } = {}) {
  const cleanHash = String(hash || "")
    .replace(/^#/, "")
    .replace(/^\/+/, "")
    .split(/[?#]/)[0]
    .trim()
    .toLowerCase();
  if (cleanHash === "login" || cleanHash === "signin") return "login";
  if (cleanHash === "signup" || cleanHash === "register" || cleanHash === "create-account") {
    return "signup";
  }
  if (cleanHash === "reset" || cleanHash === "forgot" || cleanHash === "forgot-password" || cleanHash === "reset-password") {
    return "reset";
  }

  const pathSeg = String(pathname || "")
    .replace(/^\/+/, "")
    .split("/")[0]
    .replace(/\.html$/i, "")
    .trim()
    .toLowerCase();
  if (pathSeg === "login" || pathSeg === "signin") return "login";
  if (pathSeg === "signup" || pathSeg === "register" || pathSeg === "create-account") {
    return "signup";
  }
  if (pathSeg === "reset" || pathSeg === "forgot" || pathSeg === "forgot-password" || pathSeg === "reset-password") {
    return "reset";
  }

  if (search) {
    try {
      const params = new URLSearchParams(String(search).startsWith("?") ? search : `?${search}`);
      const r = String(params.get("route") || params.get("auth") || params.get("mode") || "")
        .trim()
        .toLowerCase();
      if (r === "login" || r === "signin") return "login";
      if (r === "signup" || r === "register" || r === "create-account") return "signup";
      if (r === "reset" || r === "forgot" || r === "forgot-password" || r === "reset-password") return "reset";
    } catch {
      // Ignore malformed query string
    }
  }

  return "";
}

/**
 * Enforces mandatory account registration & login before accessing any studio service.
 * - Unauthenticated users are always redirected to "login" or "signup" (zero guest access).
 * - Authenticated members are redirected out of auth pages into their target service route,
 *   while preserving strict Admin Dashboard authorization via resolveSafeRoute.
 */
export function resolveAuthenticatedAccess({
  requestedRoute = "",
  pathname = "",
  hash = "",
  search = "",
  user = null,
  isAdmin = false,
  postAuthRoute = "home"
} = {}) {
  const normalizedReq = String(requestedRoute || "").trim().toLowerCase();
  const explicitAuthRoute =
    normalizedReq === "login" || normalizedReq === "signin"
      ? "login"
      : normalizedReq === "signup" || normalizedReq === "register"
      ? "signup"
      : normalizedReq === "reset" || normalizedReq === "forgot" || normalizedReq === "forgot-password"
      ? "reset"
      : extractAuthRouteFromUrl({ pathname, hash, search });

  const authenticated = isAuthenticatedMember(user);
  const verifiedAdmin = authenticated && Boolean(isAdmin && isAuthorizedAdminUser(user));

  if (!authenticated) {
    const authTarget = explicitAuthRoute || "login";
    const intendedService =
      normalizedReq && !isAuthRoute(normalizedReq)
        ? normalizedReq
        : !explicitAuthRoute
        ? extractRequestedRoute({ pathname, hash, search })
        : "";
    const adminRequested =
      intendedService === "admin" || isAdminUrlAttempt({ pathname, hash, search });

    return {
      authenticated: false,
      route: authTarget,
      authMode: authTarget,
      redirectedToAuth: true,
      blockedAdminAttempt: adminRequested,
      shouldNormalizeUrl: true,
      postAuthRoute:
        intendedService && !isAuthRoute(intendedService) ? intendedService : postAuthRoute || "home"
    };
  }

  const candidateRoute =
    normalizedReq && !isAuthRoute(normalizedReq)
      ? normalizedReq
      : explicitAuthRoute
      ? postAuthRoute || "home"
      : extractRequestedRoute({ pathname, hash, search });

  const safe = resolveSafeRoute({
    requestedRoute: candidateRoute,
    pathname,
    hash,
    search,
    isAdmin: verifiedAdmin
  });

  return {
    authenticated: true,
    route: safe.route,
    authMode: "login",
    redirectedToAuth: false,
    blockedAdminAttempt: safe.blockedAdminAttempt,
    shouldNormalizeUrl: safe.shouldNormalizeUrl || Boolean(explicitAuthRoute),
    postAuthRoute: safe.route
  };
}

/**
 * Validates new account registration inputs before creating a Firebase Auth account.
 */
export function validateRegistrationInput({
  fullName = "",
  email = "",
  password = "",
  confirmPassword = ""
} = {}) {
  const cleanName = String(fullName || "").trim();
  const cleanEmail = String(email || "").trim().toLowerCase();
  const rawPassword = String(password || "");
  const rawConfirm = String(confirmPassword || "");

  if (cleanName.length < 2 || cleanName.length > 100) {
    return { valid: false, errorKey: "authErrorInvalidName" };
  }
  if (!EMAIL_PATTERN.test(cleanEmail) || cleanEmail.length > 128) {
    return { valid: false, errorKey: "authErrorInvalidEmail" };
  }
  if (rawPassword.length < 6 || rawPassword.length > 128) {
    return { valid: false, errorKey: "authErrorWeakPassword" };
  }
  if (rawPassword !== rawConfirm) {
    return { valid: false, errorKey: "authErrorPasswordMismatch" };
  }

  return {
    valid: true,
    clean: {
      fullName: cleanName,
      email: cleanEmail,
      password: rawPassword
    }
  };
}

/**
 * Validates login inputs for existing registered accounts.
 */
export function validateLoginInput({ email = "", password = "" } = {}) {
  const cleanEmail = String(email || "").trim().toLowerCase();
  const rawPassword = String(password || "");

  if (!EMAIL_PATTERN.test(cleanEmail) || cleanEmail.length > 128) {
    return { valid: false, errorKey: "authErrorInvalidEmail" };
  }
  if (rawPassword.length < 6) {
    return { valid: false, errorKey: "authErrorPasswordRequired" };
  }

  return {
    valid: true,
    clean: {
      email: cleanEmail,
      password: rawPassword
    }
  };
}

/**
 * Validates email input before triggering a Firebase Password Reset email.
 */
export function validatePasswordResetInput({ email = "" } = {}) {
  const cleanEmail = String(email || "").trim().toLowerCase();
  if (!EMAIL_PATTERN.test(cleanEmail) || cleanEmail.length > 128) {
    return { valid: false, errorKey: "authErrorInvalidEmail" };
  }
  return {
    valid: true,
    clean: { email: cleanEmail }
  };
}

/**
 * Maps Firebase Auth error codes into clear, helpful messages enforcing mandatory registration.
 */
export function mapFirebaseAuthError(err, { mode = "login", t = (k) => k } = {}) {
  const code = String(err?.code || "").trim().toLowerCase();
  if (code === "auth/unregistered-account" || code === "auth/user-not-found") {
    return t("authErrorAccountNotRegistered");
  }
  if (code === "auth/invalid-credential" || code === "auth/wrong-password") {
    return mode === "login" ? t("authErrorInvalidLoginCredentials") : t("authErrorGeneric");
  }
  if (code === "auth/email-already-in-use") {
    return t("authErrorEmailAlreadyInUse");
  }
  if (code === "auth/weak-password") {
    return t("authErrorWeakPassword");
  }
  if (code === "auth/invalid-email") {
    return t("authErrorInvalidEmail");
  }
  if (code === "auth/too-many-requests") {
    return t("authErrorTooManyRequests");
  }
  if (code === "auth/popup-closed-by-user" || code === "auth/cancelled-popup-request") {
    return t("authErrorPopupCancelled");
  }
  return err?.message || t("authErrorGeneric");
}

/**
 * Synchronizes DOM visibility between the mandatory Auth Gate page (#view-auth)
 * and the protected studio services views.
 */
export function syncAuthGateDom(
  doc,
  { isAuthenticated = false, authMode = "login", authResolved = true } = {}
) {
  if (!doc || typeof doc.getElementById !== "function") return;
  const normalizedMode = authMode === "signup" ? "signup" : authMode === "reset" ? "reset" : "login";

  if (doc.body && doc.body.classList) {
    doc.body.classList.toggle("auth-gate-active", !isAuthenticated);
    doc.body.classList.toggle("auth-checking", !isAuthenticated && !authResolved);
  }

  const authView = doc.getElementById("view-auth");
  if (authView) {
    authView.classList.toggle("hidden", isAuthenticated);
    authView.hidden = isAuthenticated;
    authView.setAttribute("aria-hidden", isAuthenticated ? "true" : "false");
  }

  if (!isAuthenticated) {
    for (const viewId of ["view-home", "view-catalog", "view-booking", "view-models", "view-companies", "view-gallery", "view-admin"]) {
      const sec = doc.getElementById(viewId);
      if (!sec) continue;
      sec.classList?.toggle("hidden", true);
      sec.hidden = true;
      sec.setAttribute?.("aria-hidden", "true");
    }
  }

  const loginPanel = doc.getElementById("authLoginPanel");
  const signupPanel = doc.getElementById("authSignupPanel");
  const resetPanel = doc.getElementById("authResetPanel");
  const loginTabBtn = doc.getElementById("authTabLoginBtn");
  const signupTabBtn = doc.getElementById("authTabSignupBtn");

  if (loginPanel) {
    const showLogin = normalizedMode === "login";
    loginPanel.classList.toggle("hidden", !showLogin);
    loginPanel.hidden = !showLogin;
  }
  if (signupPanel) {
    const showSignup = normalizedMode === "signup";
    signupPanel.classList.toggle("hidden", !showSignup);
    signupPanel.hidden = !showSignup;
  }
  if (resetPanel) {
    const showReset = normalizedMode === "reset";
    resetPanel.classList.toggle("hidden", !showReset);
    resetPanel.hidden = !showReset;
  }
  if (loginTabBtn) {
    const active = normalizedMode === "login" || normalizedMode === "reset";
    loginTabBtn.classList.toggle("active", active);
    loginTabBtn.setAttribute("aria-selected", active ? "true" : "false");
  }
  if (signupTabBtn) {
    const active = normalizedMode === "signup";
    signupTabBtn.classList.toggle("active", active);
    signupTabBtn.setAttribute("aria-selected", active ? "true" : "false");
  }
}
