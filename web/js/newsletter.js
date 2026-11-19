export const NEWSLETTER_STORAGE_KEY = "star_artist_workshop_newsletter_v1";

export const EMAIL_REGEX =
  /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)+$/;

export function normalizeSubscriberEmail(email) {
  return String(email ?? "").trim().toLowerCase();
}

export function evaluateNewsletterEmailInput(email, lang = "en", { allowEmpty = true } = {}) {
  const raw = String(email ?? "");
  const clean = normalizeSubscriberEmail(raw);
  const isHi = lang === "hi";

  if (!clean) {
    if (allowEmpty) {
      return {
        state: "idle",
        isValid: false,
        normalizedEmail: "",
        error: ""
      };
    }
    return {
      state: "invalid",
      isValid: false,
      normalizedEmail: "",
      error: isHi
        ? "कृपया वर्कशॉप अपडेट्स के लिए अपना ईमेल पता दर्ज करें।"
        : "Please enter your email address for studio workshop updates."
    };
  }

  if (/\s/.test(clean)) {
    return {
      state: "invalid",
      isValid: false,
      normalizedEmail: clean,
      error: isHi
        ? "ईमेल पते में खाली स्थान (space) नहीं होना चाहिए।"
        : "Email address cannot contain spaces."
    };
  }

  if (!clean.includes("@")) {
    return {
      state: "invalid",
      isValid: false,
      normalizedEmail: clean,
      error: isHi
        ? "कृपया ईमेल पते में '@' चिह्न शामिल करें (उदा. name@example.com)।"
        : "Include an '@' symbol in the email address (e.g., name@example.com)."
    };
  }

  if (clean.length > 128 || clean.includes("..") || !EMAIL_REGEX.test(clean)) {
    return {
      state: "invalid",
      isValid: false,
      normalizedEmail: clean,
      error: isHi
        ? "कृपया मान्य ईमेल प्रारूप दर्ज करें (जैसे: artist@studio.com)।"
        : "Please enter a valid email address (e.g., artist@studio.com)."
    };
  }

  return {
    state: "valid",
    isValid: true,
    normalizedEmail: clean,
    error: ""
  };
}

export function validateSubscriberEmail(email, lang = "en") {
  const evaluation = evaluateNewsletterEmailInput(email, lang, { allowEmpty: false });
  return evaluation.isValid ? null : evaluation.error;
}

function getDefaultStorage() {
  try {
    if (typeof localStorage !== "undefined") return localStorage;
  } catch {
    // Storage disabled
  }
  return null;
}

export function loadNewsletterSubscribers(storage = getDefaultStorage()) {
  if (!storage || typeof storage.getItem !== "function") return [];
  try {
    const raw = storage.getItem(NEWSLETTER_STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function saveNewsletterSubscription({
  email,
  lang = "en",
  source = "footer_newsletter",
  storage = getDefaultStorage()
} = {}) {
  const cleanEmail = normalizeSubscriberEmail(email);
  const error = validateSubscriberEmail(email, lang);
  if (error) {
    return {
      ok: false,
      alreadySubscribed: false,
      error,
      entry: null,
      subscribers: loadNewsletterSubscribers(storage)
    };
  }

  const existingList = loadNewsletterSubscribers(storage);
  const existingEntry = existingList.find(
    (item) => normalizeSubscriberEmail(item?.email) === cleanEmail
  );

  if (existingEntry) {
    return {
      ok: true,
      alreadySubscribed: true,
      error: null,
      entry: existingEntry,
      subscribers: existingList
    };
  }

  const entry = {
    email: cleanEmail,
    interest: "studio_workshops",
    source,
    lang: lang === "hi" ? "hi" : "en",
    subscribedAt: new Date().toISOString()
  };

  const nextSubscribers = [...existingList, entry];
  if (storage && typeof storage.setItem === "function") {
    try {
      storage.setItem(NEWSLETTER_STORAGE_KEY, JSON.stringify(nextSubscribers));
    } catch {
      // Ignore storage quota errors gracefully
    }
  }

  return {
    ok: true,
    alreadySubscribed: false,
    error: null,
    entry,
    subscribers: nextSubscribers
  };
}

export const NEWSLETTER_SUCCESS_DURATION_MS = 5000;

let activeNewsletterSuccessTimer = null;

export function showTemporaryNewsletterSuccess({
  fieldsEl,
  successEl,
  detailEl = null,
  detailText = "",
  durationMs = NEWSLETTER_SUCCESS_DURATION_MS,
  scheduleTimeout = setTimeout,
  cancelTimeout = clearTimeout,
  onRestore = null
} = {}) {
  if (activeNewsletterSuccessTimer !== null) {
    cancelTimeout(activeNewsletterSuccessTimer);
    activeNewsletterSuccessTimer = null;
  }

  if (detailEl && detailText) {
    detailEl.textContent = detailText;
  }

  fieldsEl?.classList?.add("hidden");
  successEl?.classList?.remove("hidden");

  const restoreNow = () => {
    if (activeNewsletterSuccessTimer !== null) {
      cancelTimeout(activeNewsletterSuccessTimer);
      activeNewsletterSuccessTimer = null;
    }
    successEl?.classList?.add("hidden");
    fieldsEl?.classList?.remove("hidden");
    if (typeof onRestore === "function") {
      onRestore();
    }
  };

  const timerId = scheduleTimeout(() => {
    activeNewsletterSuccessTimer = null;
    successEl?.classList?.add("hidden");
    fieldsEl?.classList?.remove("hidden");
    if (typeof onRestore === "function") {
      onRestore();
    }
  }, durationMs);

  activeNewsletterSuccessTimer = timerId;

  return {
    timerId,
    restoreNow,
    cancel: () => {
      if (activeNewsletterSuccessTimer !== null) {
        cancelTimeout(activeNewsletterSuccessTimer);
        activeNewsletterSuccessTimer = null;
      }
    }
  };
}

