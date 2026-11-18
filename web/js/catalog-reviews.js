function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

export function catalogReviewBookingId(packageId) {
  return `catalog_${String(packageId || "").trim()}`;
}

export function validateCatalogReview(rating, comment, lang = "en") {
  const numRating = Number(rating);
  const isHindi = lang === "hi";
  if (!Number.isInteger(numRating) || numRating < 1 || numRating > 5) {
    return isHindi
      ? "कृपया 1 से 5 स्टार रेटिंग चुनें।"
      : "Please select a star rating between 1 and 5.";
  }
  const trimmed = String(comment ?? "").trim();
  if (trimmed.length < 5) {
    return isHindi
      ? "कृपया कम से कम 5 अक्षरों की टिप्पणी लिखें।"
      : "Please write at least 5 characters in your comment.";
  }
  if (trimmed.length > 1000) {
    return isHindi
      ? "टिप्पणी 1000 अक्षरों या उससे कम होनी चाहिए।"
      : "Comment must be 1000 characters or fewer.";
  }
  return null;
}

export function filterReviewsForPackage(reviews = [], pkg) {
  if (!pkg) return [];
  const targetBookingId = catalogReviewBookingId(pkg.id).toLowerCase();
  const rec = String(pkg.recommendedServiceType || "").trim().toLowerCase();
  const titleEn = String(pkg.titleEn || "").trim().toLowerCase();
  return reviews.filter((review) => {
    const bId = String(review?.bookingId || "").trim().toLowerCase();
    const sType = String(review?.serviceType || "").trim().toLowerCase();
    return (
      bId === targetBookingId ||
      (rec && sType.startsWith(rec)) ||
      (titleEn && sType.includes(titleEn))
    );
  });
}

export function summarizePackageReviews(reviews = []) {
  if (!reviews.length) {
    return { count: 0, average: 0, formattedAverage: "0.0" };
  }
  const sum = reviews.reduce((acc, r) => {
    const val = Math.min(5, Math.max(1, Number(r?.rating) || 5));
    return acc + val;
  }, 0);
  const average = sum / reviews.length;
  return {
    count: reviews.length,
    average,
    formattedAverage: average.toFixed(1)
  };
}

function starSvg(filled, size = 16) {
  if (filled) {
    return `<svg viewBox="0 0 20 20" width="${size}" height="${size}" fill="currentColor" aria-hidden="true"><path d="M10 1.6 12.6 6.9l5.8.85-4.2 4.1.99 5.8L10 14.9 4.81 17.65l.99-5.8-4.2-4.1 5.8-.85L10 1.6z"/></svg>`;
  }
  return `<svg viewBox="0 0 20 20" width="${size}" height="${size}" fill="none" stroke="currentColor" stroke-width="1.6" aria-hidden="true"><path d="M10 2.1 12.4 7l5.4.78-3.9 3.8.92 5.38L10 14.4l-4.82 2.56.92-5.38-3.9-3.8L7.6 7 10 2.1z"/></svg>`;
}

export function renderReadOnlyStarsHtml(rating, size = 14) {
  const clamped = Math.min(5, Math.max(0, Math.round(Number(rating) || 0)));
  let stars = "";
  for (let i = 1; i <= 5; i += 1) {
    stars += `<span class="star-glyph ${i <= clamped ? "filled" : ""}">${starSvg(i <= clamped, size)}</span>`;
  }
  return `<span class="read-only-stars" aria-label="${clamped} out of 5 stars">${stars}</span>`;
}

export function renderCatalogFeedbackHtml({
  pkg,
  reviews = [],
  existingReview = null,
  isExpanded = false,
  draftRating = null,
  draftComment = null,
  isSubmitting = false,
  lang = "en",
  t = (k) => k,
  interactive = true
}) {
  const pkgReviews = filterReviewsForPackage(reviews, pkg);
  const summary = summarizePackageReviews(pkgReviews);
  const activeRating = Math.min(
    5,
    Math.max(1, Number(draftRating ?? existingReview?.rating ?? 5) || 5)
  );
  const activeComment = String(draftComment ?? existingReview?.comment ?? "");
  const ratingBadgeText =
    summary.count > 0 ? `${summary.formattedAverage} (${summary.count})` : lang === "hi" ? "नई समीक्षा" : "New";
  const toggleLabel =
    summary.count > 0
      ? `${t("ratingsAndComments")} (${summary.count})`
      : t("rateAndComment");

  if (!interactive) {
    return `
      <div class="catalog-rating-strip">
        <span class="catalog-rating-pill">${starSvg(true, 13)} <span>${escapeHtml(ratingBadgeText)}</span></span>
        <span style="font-size:0.78rem;color:var(--text-secondary);">${escapeHtml(toggleLabel)}</span>
      </div>`;
  }

  const starsButtons = [1, 2, 3, 4, 5]
    .map((star) => {
      const active = star <= activeRating;
      return `
        <button
          type="button"
          class="star-picker-btn ${active ? "filled" : ""}"
          data-rate-pkg="${escapeHtml(pkg.id)}"
          data-rate-value="${star}"
          aria-label="Rate ${star} out of 5 stars"
          aria-pressed="${star === activeRating ? "true" : "false"}"
        >${starSvg(active, 20)}</button>`;
    })
    .join("");

  const reviewItemsHtml = pkgReviews.length
    ? pkgReviews
        .slice(0, 5)
        .map(
          (r) => `
        <div class="catalog-review-item">
          <div class="catalog-review-item-head">
            <strong>${escapeHtml(r.reviewerName || "Studio Member")}</strong>
            ${renderReadOnlyStarsHtml(r.rating, 13)}
          </div>
          <p class="catalog-review-comment">${escapeHtml(r.comment)}</p>
        </div>`
        )
        .join("")
    : `<p class="catalog-no-reviews">${escapeHtml(t("noFeedbackYet"))}</p>`;

  return `
    <div class="catalog-feedback-box" data-catalog-feedback="${escapeHtml(pkg.id)}">
      <button
        type="button"
        class="catalog-feedback-toggle"
        data-toggle-catalog-reviews="${escapeHtml(pkg.id)}"
        aria-expanded="${isExpanded ? "true" : "false"}"
      >
        <span class="catalog-feedback-toggle-left">
          <span class="catalog-rating-pill">${starSvg(true, 13)} <span>${escapeHtml(ratingBadgeText)}</span></span>
          <span>${escapeHtml(toggleLabel)}</span>
        </span>
        <span aria-hidden="true">${isExpanded ? "▲" : "▼"}</span>
      </button>
      <div class="catalog-feedback-panel ${isExpanded ? "" : "hidden"}">
        <div class="catalog-review-form">
          <div class="catalog-review-form-title">
            ${escapeHtml(existingReview ? t("updateFeedbackTitle") : t("leaveFeedbackTitle"))}
          </div>
          <div class="star-picker-row">
            <div class="star-rating-picker" role="group" aria-label="Star rating">
              ${starsButtons}
            </div>
            <span class="star-score-label">${activeRating}/5 ★</span>
          </div>
          <textarea
            class="input catalog-review-textarea"
            rows="2"
            maxlength="1000"
            data-review-comment="${escapeHtml(pkg.id)}"
            placeholder="${escapeHtml(t("feedbackPlaceholder"))}"
          >${escapeHtml(activeComment)}</textarea>
          <div class="catalog-review-form-actions">
            <span class="catalog-char-count">${activeComment.length}/1000</span>
            <button
              type="button"
              class="btn-outline catalog-submit-review-btn"
              data-submit-catalog-review="${escapeHtml(pkg.id)}"
              ${isSubmitting ? "disabled" : ""}
            >
              ${escapeHtml(existingReview ? t("updateFeedbackBtn") : t("submitFeedbackBtn"))}
            </button>
          </div>
        </div>
        <div class="catalog-review-list">
          ${reviewItemsHtml}
        </div>
      </div>
    </div>`;
}
