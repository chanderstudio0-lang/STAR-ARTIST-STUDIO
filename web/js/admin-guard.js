import { STUDIO_CONTACT } from "./data.js";

export const AUTHORIZED_ADMIN_EMAIL = String(STUDIO_CONTACT.adminEmail || "chanderstudio0@gmail.com")
  .trim()
  .toLowerCase();

export const PUBLIC_ROUTES = ["home", "catalog", "booking", "models", "companies", "gallery"];
export const ALL_ROUTES = [...PUBLIC_ROUTES, "admin"];

/**
 * Returns true ONLY when the authenticated Firebase user matches the exact authorized
 * Studio Administrator Gmail account with a verified email token.
 */
export function isAuthorizedAdminUser(user) {
  if (!user || typeof user !== "object") return false;
  const email = typeof user.email === "string" ? user.email.trim().toLowerCase() : "";
  if (!email || email !== AUTHORIZED_ADMIN_EMAIL) return false;
  return user.emailVerified === true;
}

/**
 * Detects direct URL attempts targeting any Admin route via pathname, hash, or query params
 * (e.g., /admin, /admin/, /admin/dashboard, /admin.html, #admin, #/admin, ?route=admin).
 */
export function isAdminUrlAttempt({ pathname = "", hash = "", search = "" } = {}) {
  const cleanPath = String(pathname || "").trim().toLowerCase();
  if (/^\/admin(\/|$|\.html$)/i.test(cleanPath)) {
    return true;
  }

  const cleanHash = String(hash || "")
    .replace(/^#/, "")
    .replace(/^\/+/, "")
    .split(/[?#]/)[0]
    .trim()
    .toLowerCase();
  if (cleanHash === "admin" || cleanHash.startsWith("admin/")) {
    return true;
  }

  const cleanSearch = String(search || "").trim();
  if (cleanSearch) {
    try {
      const params = new URLSearchParams(cleanSearch.startsWith("?") ? cleanSearch : `?${cleanSearch}`);
      const routeParam = String(params.get("route") || params.get("page") || params.get("view") || "")
        .trim()
        .toLowerCase();
      if (routeParam === "admin") return true;
      const adminFlag = String(params.get("admin") || "").trim().toLowerCase();
      if (adminFlag === "1" || adminFlag === "true") return true;
    } catch {
      // Ignore malformed query string
    }
  }

  return false;
}

/**
 * Extracts a canonical route key from the current browser URL (hash, pathname, or query param).
 */
export function extractRequestedRoute({ pathname = "", hash = "", search = "" } = {}) {
  if (isAdminUrlAttempt({ pathname, hash, search })) {
    return "admin";
  }

  const cleanHash = String(hash || "")
    .replace(/^#/, "")
    .replace(/^\/+/, "")
    .split(/[?#]/)[0]
    .trim()
    .toLowerCase();
  if (PUBLIC_ROUTES.includes(cleanHash)) {
    return cleanHash;
  }

  const pathSeg = String(pathname || "")
    .replace(/^\/+/, "")
    .split("/")[0]
    .replace(/\.html$/i, "")
    .trim()
    .toLowerCase();
  if (PUBLIC_ROUTES.includes(pathSeg)) {
    return pathSeg;
  }

  if (search) {
    try {
      const params = new URLSearchParams(String(search).startsWith("?") ? search : `?${search}`);
      const routeParam = String(params.get("route") || params.get("page") || "")
        .trim()
        .toLowerCase();
      if (PUBLIC_ROUTES.includes(routeParam)) {
        return routeParam;
      }
    } catch {
      // Ignore malformed query string
    }
  }

  return "home";
}

/**
 * Enforces Admin Dashboard route protection. Unauthorized attempts to access Admin
 * via UI click or direct URL are always blocked and redirected safely to "home".
 */
export function resolveSafeRoute({
  requestedRoute = "",
  pathname = "",
  hash = "",
  search = "",
  isAdmin = false
} = {}) {
  const normalizedReq = String(requestedRoute || "").trim().toLowerCase();
  const urlTargetsAdmin = isAdminUrlAttempt({ pathname, hash, search });
  const targetsAdmin = normalizedReq === "admin" || (!normalizedReq && urlTargetsAdmin);

  if (targetsAdmin) {
    if (isAdmin === true) {
      return {
        route: "admin",
        blockedAdminAttempt: false,
        shouldNormalizeUrl: /^\/admin(\/|$|\.html$)/i.test(String(pathname || "").trim())
      };
    }
    return {
      route: "home",
      blockedAdminAttempt: true,
      shouldNormalizeUrl: true
    };
  }

  const candidate = normalizedReq || extractRequestedRoute({ pathname, hash, search });
  const safeRoute = PUBLIC_ROUTES.includes(candidate) ? candidate : "home";
  return {
    route: safeRoute,
    blockedAdminAttempt: false,
    shouldNormalizeUrl: urlTargetsAdmin
  };
}

function setElementVisibility(el, visible) {
  if (!el) return;
  if (el.classList && typeof el.classList.toggle === "function") {
    el.classList.toggle("hidden", !visible);
  }
  if ("hidden" in el) {
    el.hidden = !visible;
  }
  if (typeof el.setAttribute === "function") {
    el.setAttribute("aria-hidden", visible ? "false" : "true");
  }
}

/**
 * Strictly hides all Admin navigation links, quick buttons, and dashboard sections
 * from normal visitors and wipes any rendered admin DOM when unauthorized or signed out.
 */
export function syncAdminUiVisibility(doc, { isAdmin = false, adminEmail = "" } = {}) {
  if (!doc || typeof doc.getElementById !== "function") return;

  const authorized = isAdmin === true;
  const navIds = ["desktopAdminNavBtn", "openAdminConsoleQuickBtn", "mobileAdminNavBtn"];
  for (const id of navIds) {
    setElementVisibility(doc.getElementById(id), authorized);
  }

  const adminView = doc.getElementById("view-admin");
  const adminLocked = doc.getElementById("adminLockedPanel");
  const adminContent = doc.getElementById("adminConsoleContent");
  const verifiedBadge = doc.getElementById("adminVerifiedEmailBadge");

  if (!authorized) {
    setElementVisibility(adminView, false);
    setElementVisibility(adminContent, false);
    setElementVisibility(adminLocked, true);
    const statsGrid = doc.getElementById("adminStatsGrid");
    const tabBody = doc.getElementById("adminTabBody");
    const growthMetrics = doc.getElementById("adminModelGrowthMetrics");
    const growthChart = doc.getElementById("adminModelGrowthChartMount");
    const growthStatus = doc.getElementById("adminModelGrowthStatus");
    if (statsGrid) statsGrid.innerHTML = "";
    if (tabBody) tabBody.innerHTML = "";
    if (growthMetrics) growthMetrics.innerHTML = "";
    if (growthChart) growthChart.innerHTML = "";
    if (growthStatus) growthStatus.textContent = "";
    if (verifiedBadge) verifiedBadge.textContent = "";
  } else {
    setElementVisibility(adminLocked, false);
    setElementVisibility(adminContent, true);
    if (verifiedBadge) {
      verifiedBadge.textContent = adminEmail || AUTHORIZED_ADMIN_EMAIL;
    }
  }
}
