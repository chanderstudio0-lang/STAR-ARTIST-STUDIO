/**
 * Filter-bar category count helpers and Web Share API lightbox sharing for STAR ARTIST STUDIO.
 */

export function countCatalogByCategory(services = [], searchQuery = "") {
  const q = String(searchQuery || "").trim().toLowerCase();
  const matching = services.filter((pkg) => {
    if (!q) return true;
    const text = `${pkg.titleEn || ""} ${pkg.titleHi || ""} ${pkg.summaryEn || ""} ${pkg.tagEn || ""} ${pkg.recommendedServiceType || ""}`.toLowerCase();
    return text.includes(q);
  });

  const counts = {
    all: matching.length,
    photography: 0,
    modeling: 0,
    production: 0
  };

  for (const pkg of matching) {
    const key = String(pkg.category || "").toLowerCase();
    if (Object.prototype.hasOwnProperty.call(counts, key)) {
      counts[key] += 1;
    }
  }
  return counts;
}

export function countGalleryByCategory(items = [], categories = []) {
  const counts = {};
  for (const cat of categories) {
    counts[cat] = 0;
  }
  counts.All = items.length;
  for (const item of items) {
    const cat = String(item?.category || "").trim();
    if (!cat) continue;
    counts[cat] = (counts[cat] || 0) + 1;
  }
  return counts;
}

export function buildGallerySharePayload(
  item,
  baseUrl = "https://gen-lang-client-0548683163.web.app/"
) {
  const title = String(item?.title || "Studio Showcase").trim();
  const category = String(item?.category || "Portfolio").trim();
  const rawImage = String(item?.mediaUrl || item?.thumbnailUrl || "").trim();

  let imageUrl = "";
  if (rawImage) {
    try {
      imageUrl = new URL(rawImage, baseUrl).href;
    } catch {
      imageUrl = rawImage;
    }
  }

  let pageUrl = baseUrl;
  try {
    const parsed = new URL(baseUrl);
    parsed.hash = "gallery";
    pageUrl = parsed.href;
  } catch {
    pageUrl = imageUrl || "https://gen-lang-client-0548683163.web.app/#gallery";
  }

  const slug = title
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-|-$/g, "") || "star-artist-studio";

  return {
    title: `${title} — STAR ARTIST STUDIO`,
    text: `Check out "${title}" (${category}) from STAR ARTIST STUDIO, Sirsa, Haryana.`,
    url: imageUrl || pageUrl,
    pageUrl,
    imageUrl,
    fileName: `${slug}.jpg`
  };
}

/**
 * Shares a gallery studio image via the Web Share API (with image File attachment when supported,
 * otherwise native URL/text share), falling back gracefully to clipboard copy on desktop browsers.
 */
export async function shareGalleryImage(
  item,
  {
    nav = typeof navigator !== "undefined" ? navigator : null,
    fetchImpl = typeof fetch === "function" ? fetch : null,
    FileCtor = typeof File === "function" ? File : null,
    baseUrl = typeof window !== "undefined" ? window.location.href : "https://gen-lang-client-0548683163.web.app/"
  } = {}
) {
  const payload = buildGallerySharePayload(item, baseUrl);

  if (nav && typeof nav.share === "function") {
    // Attempt Web Share Level 2 image file sharing when supported and image is fetchable
    if (payload.imageUrl && typeof nav.canShare === "function" && fetchImpl && FileCtor) {
      try {
        const res = await fetchImpl(payload.imageUrl);
        if (res && res.ok) {
          const blob = await res.blob();
          const mime = blob.type && blob.type.startsWith("image/") ? blob.type : "image/jpeg";
          const ext = mime.includes("png") ? "png" : mime.includes("webp") ? "webp" : "jpg";
          const fileName = payload.fileName.replace(/\.jpg$/, `.${ext}`);
          const imageFile = new FileCtor([blob], fileName, { type: mime });
          if (nav.canShare({ files: [imageFile] })) {
            await nav.share({
              title: payload.title,
              text: payload.text,
              url: payload.pageUrl,
              files: [imageFile]
            });
            return { status: "shared-file", payload };
          }
        }
      } catch (err) {
        if (err?.name === "AbortError") {
          return { status: "cancelled", payload };
        }
        // Fall through to standard Web Share link/text sharing
      }
    }

    try {
      await nav.share({
        title: payload.title,
        text: payload.text,
        url: payload.url
      });
      return { status: "shared-link", payload };
    } catch (err) {
      if (err?.name === "AbortError") {
        return { status: "cancelled", payload };
      }
      // Fall through to clipboard fallback if Web Share fails
    }
  }

  if (nav?.clipboard && typeof nav.clipboard.writeText === "function") {
    try {
      await nav.clipboard.writeText(`${payload.title}\n${payload.text}\n${payload.url}`);
      return { status: "copied", payload };
    } catch {
      return { status: "unsupported", payload };
    }
  }

  return { status: "unsupported", payload };
}
