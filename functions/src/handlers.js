import { resolveSmtpConfig } from "./config.js";
import {
  buildNewBookingAlertEmail,
  buildNewModelApplicationAlertEmail,
  buildNewCompanyBookingAlertEmail
} from "./email-templates.js";
import { sendAdminAlertEmail } from "./mailer.js";

async function resolveRuntimeOverrides({ templateOverrides = null, loadTemplateOverrides = null, logger = console }) {
  if (templateOverrides && typeof templateOverrides === "object") {
    return templateOverrides;
  }
  if (typeof loadTemplateOverrides === "function") {
    try {
      const loaded = await loadTemplateOverrides();
      if (loaded && typeof loaded === "object") {
        return loaded;
      }
    } catch (err) {
      logger.warn?.("Could not load custom email template overrides; falling back to defaults.", {
        error: err?.message || String(err)
      });
    }
  }
  return {};
}

/**
 * Core handler invoked when a new document is created in bookings/{bookingId}.
 */
export async function handleNewBookingCreated(
  event,
  {
    env = process.env,
    smtpUser = "",
    smtpPass = "",
    transporter = null,
    sendMailImpl = sendAdminAlertEmail,
    templateOverrides = null,
    loadTemplateOverrides = null,
    logger = console
  } = {}
) {
  const booking = event?.data?.data?.();
  const bookingId = event?.params?.bookingId || event?.data?.id || "";

  if (!booking || typeof booking !== "object") {
    logger.warn("Skipping booking alert: empty Firestore document snapshot.", { bookingId });
    return { sent: false, reason: "empty_snapshot", type: "booking", docId: bookingId };
  }

  const smtpConfig = resolveSmtpConfig(env, { smtpUser, smtpPass });
  const overrides = await resolveRuntimeOverrides({ templateOverrides, loadTemplateOverrides, logger });
  const emailPayload = buildNewBookingAlertEmail({
    bookingId,
    booking,
    dashboardUrl: smtpConfig.dashboardUrl,
    env,
    templateOverrides: overrides
  });

  const delivery = await sendMailImpl({
    ...emailPayload,
    smtpConfig,
    transporter
  });

  logger.info("Sent admin email alert for new booking.", {
    bookingId,
    recipient: delivery.recipient,
    messageId: delivery.messageId
  });

  return {
    sent: true,
    type: "booking",
    docId: bookingId,
    recipient: delivery.recipient,
    messageId: delivery.messageId,
    subject: emailPayload.subject
  };
}

/**
 * Core handler invoked when a new document is created in models/{modelId}.
 */
export async function handleNewModelApplicationCreated(
  event,
  {
    env = process.env,
    smtpUser = "",
    smtpPass = "",
    transporter = null,
    sendMailImpl = sendAdminAlertEmail,
    templateOverrides = null,
    loadTemplateOverrides = null,
    logger = console
  } = {}
) {
  const model = event?.data?.data?.();
  const modelId = event?.params?.modelId || event?.data?.id || "";

  if (!model || typeof model !== "object") {
    logger.warn("Skipping model alert: empty Firestore document snapshot.", { modelId });
    return { sent: false, reason: "empty_snapshot", type: "model", docId: modelId };
  }

  const smtpConfig = resolveSmtpConfig(env, { smtpUser, smtpPass });
  const overrides = await resolveRuntimeOverrides({ templateOverrides, loadTemplateOverrides, logger });
  const emailPayload = buildNewModelApplicationAlertEmail({
    modelId,
    model,
    dashboardUrl: smtpConfig.dashboardUrl,
    env,
    templateOverrides: overrides
  });

  const delivery = await sendMailImpl({
    ...emailPayload,
    smtpConfig,
    transporter
  });

  logger.info("Sent admin email alert for new 18+ model application.", {
    modelId,
    recipient: delivery.recipient,
    messageId: delivery.messageId
  });

  return {
    sent: true,
    type: "model",
    docId: modelId,
    recipient: delivery.recipient,
    messageId: delivery.messageId,
    subject: emailPayload.subject
  };
}

/**
 * Core handler invoked when a new document is created in companyBookings/{requestId}.
 */
export async function handleNewCompanyBookingCreated(
  event,
  {
    env = process.env,
    smtpUser = "",
    smtpPass = "",
    transporter = null,
    sendMailImpl = sendAdminAlertEmail,
    templateOverrides = null,
    loadTemplateOverrides = null,
    logger = console
  } = {}
) {
  const request = event?.data?.data?.();
  const requestId = event?.params?.requestId || event?.data?.id || "";

  if (!request || typeof request !== "object") {
    logger.warn("Skipping company booking alert: empty Firestore document snapshot.", { requestId });
    return { sent: false, reason: "empty_snapshot", type: "companyBooking", docId: requestId };
  }

  const smtpConfig = resolveSmtpConfig(env, { smtpUser, smtpPass });
  const overrides = await resolveRuntimeOverrides({ templateOverrides, loadTemplateOverrides, logger });
  const emailPayload = buildNewCompanyBookingAlertEmail({
    requestId,
    request,
    dashboardUrl: smtpConfig.dashboardUrl,
    env,
    templateOverrides: overrides
  });

  const delivery = await sendMailImpl({
    ...emailPayload,
    smtpConfig,
    transporter
  });

  logger.info("Sent admin email alert for new company model requirement booking.", {
    requestId,
    recipient: delivery.recipient,
    messageId: delivery.messageId
  });

  return {
    sent: true,
    type: "companyBooking",
    docId: requestId,
    recipient: delivery.recipient,
    messageId: delivery.messageId,
    subject: emailPayload.subject
  };
}
