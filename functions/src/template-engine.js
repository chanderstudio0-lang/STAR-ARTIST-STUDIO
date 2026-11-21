import fs from "node:fs";
import path from "node:path";

const DEFAULT_TEMPLATES_DIR = path.resolve(import.meta.dirname, "..", "templates");
const SAFE_TEMPLATE_NAME_RE = /^[a-z0-9][a-z0-9_-]*\.html$/i;

const templateCache = new Map();

/**
 * Escapes untrusted Firestore strings for safe rendering inside HTML email bodies.
 */
export function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

/**
 * Strips CR/LF and control characters to prevent SMTP header injection in Subject lines.
 */
export function sanitizeHeaderValue(value, maxLen = 140) {
  return String(value ?? "")
    .replace(/[\r\n\t\x00-\x1f\x7f]+/g, " ")
    .replace(/\s+/g, " ")
    .trim()
    .slice(0, maxLen);
}

/**
 * Validates and loads an HTML template file from disk with optional inline override.
 * Prevents path traversal by enforcing a strict filename allow-pattern.
 */
export function loadHtmlTemplate(
  templateName,
  { templateDir = DEFAULT_TEMPLATES_DIR, inlineOverride = "" } = {}
) {
  const trimmedOverride = typeof inlineOverride === "string" ? inlineOverride.trim() : "";
  if (trimmedOverride) {
    return trimmedOverride;
  }

  if (!SAFE_TEMPLATE_NAME_RE.test(templateName)) {
    throw new Error(`Invalid template filename: ${templateName}`);
  }

  const resolvedDir = path.resolve(templateDir || DEFAULT_TEMPLATES_DIR);
  const fullPath = path.resolve(resolvedDir, templateName);
  if (!fullPath.startsWith(resolvedDir + path.sep) && fullPath !== path.join(resolvedDir, templateName)) {
    throw new Error(`Template path escapes template directory: ${templateName}`);
  }

  const cacheKey = fullPath;
  if (templateCache.has(cacheKey)) {
    return templateCache.get(cacheKey);
  }

  const fallbackPath = path.join(DEFAULT_TEMPLATES_DIR, templateName);
  const targetPath = fs.existsSync(fullPath) ? fullPath : fallbackPath;
  const content = fs.readFileSync(targetPath, "utf8");
  templateCache.set(cacheKey, content);
  return content;
}

/**
 * Renders a customizable HTML template using mustache-style placeholders:
 * - `{{key}}`     -> HTML-escaped string value (safe for all untrusted Firestore fields)
 * - `{{{key}}}`   -> Trusted pre-escaped internal HTML partial only
 */
export function renderHtmlTemplate(templateSource, variables = {}, rawPartials = {}) {
  const source = String(templateSource ?? "");

  // 1. Replace triple-brace trusted HTML partials first
  const withPartials = source.replace(/\{\{\{\s*([a-zA-Z0-9_]+)\s*\}\}\}/g, (_match, key) => {
    if (Object.prototype.hasOwnProperty.call(rawPartials, key)) {
      return String(rawPartials[key] ?? "");
    }
    return "";
  });

  // 2. Replace double-brace placeholders with HTML-escaped values
  return withPartials.replace(/\{\{\s*([a-zA-Z0-9_]+)\s*\}\}/g, (_match, key) => {
    if (Object.prototype.hasOwnProperty.call(variables, key)) {
      return escapeHtml(variables[key]);
    }
    return "";
  });
}

/**
 * Renders a customizable email Subject template and strictly strips CR/LF header injection.
 */
export function renderSubjectTemplate(templateSource, variables = {}, maxLen = 160) {
  const raw = String(templateSource ?? "").replace(/\{\{\s*([a-zA-Z0-9_]+)\s*\}\}/g, (_match, key) => {
    if (Object.prototype.hasOwnProperty.call(variables, key)) {
      return String(variables[key] ?? "");
    }
    return "";
  });
  return sanitizeHeaderValue(raw, maxLen);
}

/**
 * Clears cached template files (useful in automated tests or hot-reload setups).
 */
export function clearTemplateCache() {
  templateCache.clear();
}

export { DEFAULT_TEMPLATES_DIR };
