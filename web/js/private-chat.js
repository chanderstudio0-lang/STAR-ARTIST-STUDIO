import { formatNetworkTimestamp } from "./pro-network.js";

export const MAX_CHAT_IMAGE_CHARS = 180000;
export const ALLOWED_CHAT_CONTEXT_TYPES = ["hiring", "professional", "outsourcing", "general"];

function defaultEscapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

export function buildChatThreadId({ participantUid = "", contextType = "general", contextId = "" } = {}) {
  const uidPart = String(participantUid || "").replace(/[^a-zA-Z0-9_-]/g, "").slice(0, 48);
  const safeType = ALLOWED_CHAT_CONTEXT_TYPES.includes(contextType) ? contextType : "general";
  const ctxPart = String(contextId || "studio").replace(/[^a-zA-Z0-9_-]/g, "").slice(0, 48) || "studio";
  return `chat_${uidPart}_${safeType}_${ctxPart}`.slice(0, 128);
}

export function isAllowedChatImageDataUrl(value) {
  const clean = String(value || "").trim();
  if (!clean) return true;
  if (clean.length > MAX_CHAT_IMAGE_CHARS) return false;
  return /^data:image\/(jpeg|jpg|png|webp);base64,/i.test(clean);
}

export function validatePrivateChatMessage({ text = "", imageAttachment = "" } = {}) {
  const cleanText = String(text || "").trim();
  const cleanImg = String(imageAttachment || "").trim();
  if (!cleanText && !cleanImg) {
    return { valid: false, error: "Enter a message or attach an image." };
  }
  if (cleanText.length > 2000) {
    return { valid: false, error: "Message cannot exceed 2000 characters." };
  }
  if (cleanImg && !isAllowedChatImageDataUrl(cleanImg)) {
    return { valid: false, error: "Invalid or oversized image attachment (max 150 KB compressed)." };
  }
  return {
    valid: true,
    clean: {
      text: cleanText,
      imageAttachment: cleanImg
    }
  };
}

/**
 * Enforces strict private conversation isolation client-side (backed by firestore.rules server-side).
 */
export function canUserAccessChatThread(thread, user, isAdmin = Boolean(user?.isAdmin)) {
  if (!thread || !user || !user.uid) return false;
  if (isAdmin === true) return true;
  return String(thread.participantUid || "") === String(user.uid);
}

export function countUnreadChatsForUser(threads = []) {
  return (Array.isArray(threads) ? threads : []).reduce(
    (sum, th) => sum + Math.max(0, Number(th?.unreadByUser || 0)),
    0
  );
}

export function countUnreadChatsForAdmin(threads = []) {
  return (Array.isArray(threads) ? threads : []).reduce(
    (sum, th) => sum + Math.max(0, Number(th?.unreadByAdmin || 0)),
    0
  );
}

/**
 * Computes visual read receipt state for a chat message bubble.
 * Shown only on the sender's own outgoing messages: single check when sent, double check when read by recipient.
 */
export function getMessageReadReceiptState(msg, { viewerRole = "customer" } = {}) {
  if (!msg || typeof msg !== "object") {
    return { show: false, status: "none", isRead: false, symbol: "" };
  }
  const isMine =
    (viewerRole === "admin" && msg.senderRole === "admin") ||
    (viewerRole !== "admin" && msg.senderRole !== "admin");
  if (!isMine) {
    return { show: false, status: "none", isRead: false, symbol: "" };
  }
  const isRead = msg.isRead === true || Boolean(msg.readAt);
  return {
    show: true,
    status: isRead ? "read" : "sent",
    isRead,
    symbol: isRead ? "✓✓" : "✓"
  };
}

/**
 * Returns IDs of incoming messages sent by the other party that have not yet been marked read.
 */
export function getUnreadIncomingMessageIds(messages = [], { viewerRole = "customer" } = {}) {
  if (!Array.isArray(messages) || !messages.length) return [];
  const isAdminViewer = viewerRole === "admin";
  return messages
    .filter((msg) => {
      if (!msg || !msg.id) return false;
      const isIncoming = isAdminViewer ? msg.senderRole !== "admin" : msg.senderRole === "admin";
      if (!isIncoming) return false;
      return msg.isRead !== true && !msg.readAt;
    })
    .map((msg) => String(msg.id));
}

function renderReadReceiptBadgeHtml(msg, { viewerRole = "customer", lang = "en", t = (k) => k, escapeHtml = defaultEscapeHtml } = {}) {
  const receipt = getMessageReadReceiptState(msg, { viewerRole });
  if (!receipt.show) return "";
  const readTimeLabel = receipt.isRead && msg.readAt ? formatNetworkTimestamp(msg.readAt, lang) : "";
  const statusLabel = receipt.isRead
    ? readTimeLabel
      ? `${t("chatReceiptRead")} • ${readTimeLabel}`
      : t("chatReceiptRead")
    : t("chatReceiptSent");
  const svgIcon = receipt.isRead
    ? `<svg class="chat-receipt-svg" width="15" height="11" viewBox="0 0 18 12" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="1.5 6.5 4.8 9.8 11.2 2.4"></polyline><polyline points="6.6 6.5 9.9 9.8 16.5 2.4"></polyline></svg>`
    : `<svg class="chat-receipt-svg" width="12" height="11" viewBox="0 0 14 12" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="1.8 6.5 5.2 9.8 12.2 2.4"></polyline></svg>`;
  return `<span class="chat-read-receipt is-${receipt.status}" data-read-receipt="${receipt.status}" title="${escapeHtml(statusLabel)}" aria-label="${escapeHtml(statusLabel)}">${svgIcon}</span>`;
}

function renderMessageBubbleHtml(msg, { viewerRole = "customer", lang = "en", t = (k) => k, escapeHtml = defaultEscapeHtml } = {}) {
  const isMine =
    (viewerRole === "admin" && msg.senderRole === "admin") ||
    (viewerRole !== "admin" && msg.senderRole !== "admin");
  const safeImg = msg.imageAttachment && isAllowedChatImageDataUrl(msg.imageAttachment) ? msg.imageAttachment : "";
  const receiptHtml = renderReadReceiptBadgeHtml(msg, { viewerRole, lang, t, escapeHtml });
  return `
    <div class="chat-bubble-row ${isMine ? "is-mine" : "is-theirs"}">
      <div class="chat-bubble ${isMine ? "is-mine" : "is-theirs"}">
        <div class="chat-bubble-sender">${escapeHtml(msg.senderName || (msg.senderRole === "admin" ? "Studio Admin" : "Member"))}</div>
        ${msg.text ? `<div class="chat-bubble-text">${escapeHtml(msg.text)}</div>` : ""}
        ${safeImg ? `<img class="chat-bubble-img" src="${escapeHtml(safeImg)}" alt="Chat attachment" loading="lazy" />` : ""}
        <div class="chat-bubble-footer">
          <span class="chat-bubble-time">${escapeHtml(formatNetworkTimestamp(msg.createdAt, lang))}</span>
          ${receiptHtml}
        </div>
      </div>
    </div>`;
}

export function renderUserPrivateChatHtml({
  threads = [],
  activeChatId = "",
  messages = [],
  currentUserUid = "",
  draftText = "",
  draftImage = "",
  isBlocked = false,
  blockReason = "",
  isSending = false,
  lang = "en",
  t = (k) => k,
  escapeHtml = defaultEscapeHtml
} = {}) {
  const safeThreads = (Array.isArray(threads) ? threads : []).filter((th) =>
    currentUserUid ? String(th?.participantUid || "") === String(currentUserUid) : true
  );
  const activeThread = safeThreads.find((th) => th.id === activeChatId) || safeThreads[0] || null;
  const unreadTotal = countUnreadChatsForUser(safeThreads);

  const threadListHtml = safeThreads.length
    ? safeThreads
        .map((th) => {
          const isActive = activeThread && th.id === activeThread.id;
          const unread = Math.max(0, Number(th.unreadByUser || 0));
          return `
          <button type="button" class="chat-thread-item ${isActive ? "active" : ""}" data-select-chat-id="${escapeHtml(th.id)}">
            <div class="chat-thread-top">
              <strong>${escapeHtml(th.contextLabel || t("chatGeneralThreadLabel"))}</strong>
              ${unread > 0 ? `<span class="chat-unread-pill">${unread}</span>` : ""}
            </div>
            <div class="chat-thread-preview">${escapeHtml(th.lastMessageText || t("chatTapToOpen"))}</div>
            <div class="chat-thread-meta">
              <span class="badge">${escapeHtml(th.contextType || "general")}</span>
              <span>${escapeHtml(formatNetworkTimestamp(th.lastMessageAt || th.updatedAt, lang))}</span>
            </div>
          </button>`;
        })
        .join("")
    : `<div class="chat-empty-note">${escapeHtml(t("chatNoThreadsYet"))}</div>`;

  const messagesHtml = activeThread
    ? (messages || []).length
      ? messages.map((m) => renderMessageBubbleHtml(m, { viewerRole: "user", lang, t, escapeHtml })).join("")
      : `<div class="chat-empty-note">${escapeHtml(t("chatStartFirstMsg"))}</div>`
    : `<div class="chat-empty-note">${escapeHtml(t("chatSelectOrStart"))}</div>`;

  return `
    <div class="pro-chat-shell studio-card">
      <div class="pro-chat-header">
        <div>
          <span class="eyebrow">${escapeHtml(t("chatEyebrow"))}</span>
          <h3 style="margin:4px 0 2px;color:var(--gold);">${escapeHtml(t("chatHeading"))} ${unreadTotal > 0 ? `<span class="chat-unread-pill">${unreadTotal}</span>` : ""}</h3>
          <p style="margin:0;font-size:0.86rem;color:var(--text-secondary);">${escapeHtml(t("chatPrivacyBanner"))}</p>
        </div>
        <button
          type="button"
          class="btn-gold btn-sm"
          data-open-private-chat="true"
          data-chat-context-type="general"
          data-chat-context-id="studio"
          data-chat-context-label="${escapeHtml(t("chatGeneralThreadLabel"))}"
          data-chat-role="customer"
          ${isBlocked ? "disabled" : ""}
        >+ ${escapeHtml(t("chatNewAdminThreadBtn"))}</button>
      </div>

      ${
        isBlocked
          ? `<div class="chat-blocked-banner" role="alert">
              <strong>${escapeHtml(t("chatBlockedTitle"))}</strong>
              <p style="margin:4px 0 0;">${escapeHtml(blockReason || t("chatBlockedDefaultDesc"))}</p>
            </div>`
          : ""
      }

      <div class="pro-chat-grid">
        <aside class="pro-chat-sidebar" aria-label="Conversations">
          <div class="pro-chat-sidebar-title">${escapeHtml(t("chatConversationsTitle"))} (${threads.length})</div>
          <div class="pro-chat-thread-list">${threadListHtml}</div>
        </aside>

        <div class="pro-chat-conversation">
          ${
            activeThread
              ? `<div class="pro-chat-active-bar">
                  <strong>${escapeHtml(activeThread.contextLabel || t("chatGeneralThreadLabel"))}</strong>
                  <span class="badge">${escapeHtml(activeThread.contextType || "general")}</span>
                </div>`
              : ""
          }
          <div id="userPrivateChatTimeline" class="pro-chat-timeline">${messagesHtml}</div>
          ${
            activeThread && !isBlocked
              ? `<form id="userPrivateChatForm" class="pro-chat-composer" data-chat-id="${escapeHtml(activeThread.id)}">
                  ${
                    draftImage
                      ? `<div class="chat-draft-img-preview">
                          <img src="${escapeHtml(draftImage)}" alt="Attachment preview" />
                          <button type="button" class="chip-btn" data-chat-clear-image="user">✕ ${escapeHtml(t("removePhotoBtn"))}</button>
                        </div>`
                      : ""
                  }
                  <div class="pro-chat-composer-row">
                    <label class="icon-btn chat-attach-btn" title="${escapeHtml(t("chatAttachImageLabel"))}">
                      📎
                      <input type="file" accept="image/jpeg,image/png,image/webp" class="hidden" data-chat-image-input="user" />
                    </label>
                    <input
                      id="userPrivateChatInput"
                      type="text"
                      class="input"
                      maxlength="2000"
                      value="${escapeHtml(draftText)}"
                      placeholder="${escapeHtml(t("chatInputPlaceholder"))}"
                    />
                    <button type="submit" class="btn-gold btn-sm" ${isSending ? "disabled" : ""}>${escapeHtml(isSending ? t("chatSendingBtn") : t("chatSendBtn"))}</button>
                  </div>
                </form>`
              : ""
          }
        </div>
      </div>
    </div>`;
}

export function renderAdminPrivateMessagesHtml({
  threads = [],
  activeChatId = "",
  messages = [],
  blockedMap = {},
  draftText = "",
  draftImage = "",
  isSending = false,
  lang = "en",
  t = (k) => k,
  escapeHtml = defaultEscapeHtml
} = {}) {
  const activeThread = (threads || []).find((th) => th.id === activeChatId) || threads[0] || null;
  const activeBlocked = activeThread ? Boolean(blockedMap?.[activeThread.participantUid]?.isBlocked) : false;

  const threadListHtml = (threads || []).length
    ? threads
        .map((th) => {
          const isActive = activeThread && th.id === activeThread.id;
          const unread = Math.max(0, Number(th.unreadByAdmin || 0));
          const blocked = Boolean(blockedMap?.[th.participantUid]?.isBlocked);
          return `
          <button type="button" class="chat-thread-item ${isActive ? "active" : ""}" data-admin-select-chat-id="${escapeHtml(th.id)}">
            <div class="chat-thread-top">
              <strong>${escapeHtml(th.participantName || "Member")} ${blocked ? "🚫" : ""}</strong>
              ${unread > 0 ? `<span class="chat-unread-pill">${unread}</span>` : ""}
            </div>
            <div class="chat-thread-sub">${escapeHtml(th.contextLabel || th.contextType || "general")}</div>
            <div class="chat-thread-preview">${escapeHtml(th.lastMessageText || "")}</div>
            <div class="chat-thread-meta">
              <span class="badge">${escapeHtml(th.participantRole || "customer")}</span>
              <span>${escapeHtml(formatNetworkTimestamp(th.lastMessageAt || th.updatedAt, lang))}</span>
            </div>
          </button>`;
        })
        .join("")
    : `<div class="chat-empty-note">${escapeHtml(t("adminChatNoThreads"))}</div>`;

  const messagesHtml = activeThread
    ? (messages || []).length
      ? messages.map((m) => renderMessageBubbleHtml(m, { viewerRole: "admin", lang, t, escapeHtml })).join("")
      : `<div class="chat-empty-note">${escapeHtml(t("chatStartFirstMsg"))}</div>`
    : `<div class="chat-empty-note">${escapeHtml(t("adminChatSelectThread"))}</div>`;

  return `
    <div class="pro-chat-shell studio-card">
      <div class="pro-chat-header">
        <div>
          <span class="eyebrow">${escapeHtml(t("adminChatEyebrow"))}</span>
          <h3 style="margin:4px 0 2px;color:var(--gold);">${escapeHtml(t("adminTabPrivateMessages"))}</h3>
          <p style="margin:0;font-size:0.86rem;color:var(--text-secondary);">${escapeHtml(t("adminChatSubtitle"))}</p>
        </div>
      </div>

      <div class="pro-chat-grid">
        <aside class="pro-chat-sidebar" aria-label="Admin conversations">
          <div class="pro-chat-sidebar-title">${escapeHtml(t("chatConversationsTitle"))} (${threads.length})</div>
          <div class="pro-chat-thread-list">${threadListHtml}</div>
        </aside>

        <div class="pro-chat-conversation">
          ${
            activeThread
              ? `<div class="pro-chat-active-bar">
                  <div>
                    <strong>${escapeHtml(activeThread.participantName || "Member")}</strong>
                    <div style="font-size:0.78rem;color:var(--text-muted);">${escapeHtml(activeThread.participantEmail || activeThread.participantUid)} • ${escapeHtml(activeThread.contextLabel || "")}</div>
                  </div>
                  <button
                    type="button"
                    class="${activeBlocked ? "btn-outline btn-sm" : "btn-danger"}"
                    data-admin-toggle-chat-block-uid="${escapeHtml(activeThread.participantUid)}"
                    data-admin-toggle-chat-block-next="${activeBlocked ? "false" : "true"}"
                  >${escapeHtml(activeBlocked ? t("adminChatUnblockBtn") : t("adminChatBlockBtn"))}</button>
                </div>`
              : ""
          }
          <div id="adminPrivateChatTimeline" class="pro-chat-timeline">${messagesHtml}</div>
          ${
            activeThread
              ? `<form id="adminPrivateChatForm" class="pro-chat-composer" data-chat-id="${escapeHtml(activeThread.id)}">
                  ${
                    draftImage
                      ? `<div class="chat-draft-img-preview">
                          <img src="${escapeHtml(draftImage)}" alt="Attachment preview" />
                          <button type="button" class="chip-btn" data-chat-clear-image="admin">✕ ${escapeHtml(t("removePhotoBtn"))}</button>
                        </div>`
                      : ""
                  }
                  <div class="pro-chat-composer-row">
                    <label class="icon-btn chat-attach-btn" title="${escapeHtml(t("chatAttachImageLabel"))}">
                      📎
                      <input type="file" accept="image/jpeg,image/png,image/webp" class="hidden" data-chat-image-input="admin" />
                    </label>
                    <input
                      id="adminPrivateChatInput"
                      type="text"
                      class="input"
                      maxlength="2000"
                      value="${escapeHtml(draftText)}"
                      placeholder="${escapeHtml(t("adminChatReplyPlaceholder"))}"
                    />
                    <button type="submit" class="btn-gold btn-sm" ${isSending ? "disabled" : ""}>${escapeHtml(isSending ? t("chatSendingBtn") : t("adminChatReplyBtn"))}</button>
                  </div>
                </form>`
              : ""
          }
        </div>
      </div>
    </div>`;
}
