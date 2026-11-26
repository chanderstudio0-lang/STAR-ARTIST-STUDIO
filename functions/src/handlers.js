import { resolveSmtpConfig } from "./config.js";
import {
  buildNewBookingAlertEmail,
  buildNewModelApplicationAlertEmail,
  buildNewCompanyBookingAlertEmail
} from "./email-templates.js";
import { sendAdminAlertEmail, verifySmtpTransport } from "./mailer.js";

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

async function resolveRuntimeAlertSettings({ alertSettings = null, loadAlertSettings = null, logger = console }) {
  const defaults = {
    bookingAlertsEnabled: true,
    modelAlertsEnabled: true,
    companyAlertsEnabled: true
  };
  let raw = null;
  if (alertSettings && typeof alertSettings === "object") {
    raw = alertSettings;
  } else if (typeof loadAlertSettings === "function") {
    try {
      const loaded = await loadAlertSettings();
      if (loaded && typeof loaded === "object") {
        raw = loaded;
      }
    } catch (err) {
      logger.warn?.("Could not load email alert toggle settings; using enabled defaults.", {
        error: err?.message || String(err)
      });
    }
  }
  if (!raw) return defaults;
  return {
    bookingAlertsEnabled: raw.bookingAlertsEnabled !== false,
    modelAlertsEnabled: raw.modelAlertsEnabled !== false,
    companyAlertsEnabled: raw.companyAlertsEnabled !== false
  };
}

async function safeRecordAlertLog(recordAlertLog, entry, logger = console) {
  if (typeof recordAlertLog !== "function") return;
  try {
    await recordAlertLog(entry);
  } catch (err) {
    logger.warn?.("Unable to write adminEmailAlerts audit log entry.", {
      error: err?.message || String(err)
    });
  }
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
    alertSettings = null,
    loadAlertSettings = null,
    recordAlertLog = null,
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
  const toggles = await resolveRuntimeAlertSettings({ alertSettings, loadAlertSettings, logger });
  const emailPayload = buildNewBookingAlertEmail({
    bookingId,
    booking,
    dashboardUrl: smtpConfig.dashboardUrl,
    env,
    templateOverrides: overrides
  });
  const summary = `${booking.clientName || "Client"} • ${booking.serviceType || "Studio Shoot"} (${booking.preferredDate || "TBD"})`.slice(0, 480);

  if (!toggles.bookingAlertsEnabled) {
    logger.info("Booking email alert skipped (disabled in Admin Dashboard settings).", { bookingId });
    await safeRecordAlertLog(
      recordAlertLog,
      {
        type: "booking",
        recordId: bookingId || `bk_${Date.now()}`,
        recipientEmail: smtpConfig.adminEmail,
        subject: emailPayload.subject,
        summary,
        status: "skipped",
        messageId: "",
        errorMessage: "Disabled by Studio Admin settings.",
        source: "cloud-function",
        isRead: false
      },
      logger
    );
    return {
      sent: false,
      reason: "disabled_by_admin",
      type: "booking",
      docId: bookingId,
      recipient: smtpConfig.adminEmail,
      subject: emailPayload.subject
    };
  }

  try {
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

    await safeRecordAlertLog(
      recordAlertLog,
      {
        type: "booking",
        recordId: bookingId || `bk_${Date.now()}`,
        recipientEmail: delivery.recipient,
        subject: emailPayload.subject,
        summary,
        status: "sent",
        messageId: delivery.messageId || "",
        errorMessage: "",
        source: "cloud-function",
        isRead: false
      },
      logger
    );

    return {
      sent: true,
      type: "booking",
      docId: bookingId,
      recipient: delivery.recipient,
      messageId: delivery.messageId,
      subject: emailPayload.subject
    };
  } catch (err) {
    const safeError = sanitizeSmtpErrorMessage(err, smtpConfig.pass);
    await safeRecordAlertLog(
      recordAlertLog,
      {
        type: "booking",
        recordId: bookingId || `bk_${Date.now()}`,
        recipientEmail: smtpConfig.adminEmail,
        subject: emailPayload.subject,
        summary,
        status: "failed",
        messageId: "",
        errorMessage: safeError,
        source: "cloud-function",
        isRead: false
      },
      logger
    );
    throw err;
  }
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
    alertSettings = null,
    loadAlertSettings = null,
    recordAlertLog = null,
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
  const toggles = await resolveRuntimeAlertSettings({ alertSettings, loadAlertSettings, logger });
  const emailPayload = buildNewModelApplicationAlertEmail({
    modelId,
    model,
    dashboardUrl: smtpConfig.dashboardUrl,
    env,
    templateOverrides: overrides
  });
  const summary = `${model.fullName || "Applicant"} (${model.age || "18+"} yrs) • ${model.city || "India"}`.slice(0, 480);

  if (!toggles.modelAlertsEnabled) {
    logger.info("Model application email alert skipped (disabled in Admin Dashboard settings).", { modelId });
    await safeRecordAlertLog(
      recordAlertLog,
      {
        type: "model",
        recordId: modelId || `mdl_${Date.now()}`,
        recipientEmail: smtpConfig.adminEmail,
        subject: emailPayload.subject,
        summary,
        status: "skipped",
        messageId: "",
        errorMessage: "Disabled by Studio Admin settings.",
        source: "cloud-function",
        isRead: false
      },
      logger
    );
    return {
      sent: false,
      reason: "disabled_by_admin",
      type: "model",
      docId: modelId,
      recipient: smtpConfig.adminEmail,
      subject: emailPayload.subject
    };
  }

  try {
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

    await safeRecordAlertLog(
      recordAlertLog,
      {
        type: "model",
        recordId: modelId || `mdl_${Date.now()}`,
        recipientEmail: delivery.recipient,
        subject: emailPayload.subject,
        summary,
        status: "sent",
        messageId: delivery.messageId || "",
        errorMessage: "",
        source: "cloud-function",
        isRead: false
      },
      logger
    );

    return {
      sent: true,
      type: "model",
      docId: modelId,
      recipient: delivery.recipient,
      messageId: delivery.messageId,
      subject: emailPayload.subject
    };
  } catch (err) {
    const safeError = sanitizeSmtpErrorMessage(err, smtpConfig.pass);
    await safeRecordAlertLog(
      recordAlertLog,
      {
        type: "model",
        recordId: modelId || `mdl_${Date.now()}`,
        recipientEmail: smtpConfig.adminEmail,
        subject: emailPayload.subject,
        summary,
        status: "failed",
        messageId: "",
        errorMessage: safeError,
        source: "cloud-function",
        isRead: false
      },
      logger
    );
    throw err;
  }
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
    alertSettings = null,
    loadAlertSettings = null,
    recordAlertLog = null,
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
  const toggles = await resolveRuntimeAlertSettings({ alertSettings, loadAlertSettings, logger });
  const emailPayload = buildNewCompanyBookingAlertEmail({
    requestId,
    request,
    dashboardUrl: smtpConfig.dashboardUrl,
    env,
    templateOverrides: overrides
  });
  const summary = `${request.companyName || "Company"} • ${request.modelsCount || 1} ${request.modelRequirement || "Models"} (${request.eventDate || "TBD"})`.slice(0, 480);

  if (!toggles.companyAlertsEnabled) {
    logger.info("Company booking email alert skipped (disabled in Admin Dashboard settings).", { requestId });
    await safeRecordAlertLog(
      recordAlertLog,
      {
        type: "company",
        recordId: requestId || `cmp_${Date.now()}`,
        recipientEmail: smtpConfig.adminEmail,
        subject: emailPayload.subject,
        summary,
        status: "skipped",
        messageId: "",
        errorMessage: "Disabled by Studio Admin settings.",
        source: "cloud-function",
        isRead: false
      },
      logger
    );
    return {
      sent: false,
      reason: "disabled_by_admin",
      type: "companyBooking",
      docId: requestId,
      recipient: smtpConfig.adminEmail,
      subject: emailPayload.subject
    };
  }

  try {
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

    await safeRecordAlertLog(
      recordAlertLog,
      {
        type: "company",
        recordId: requestId || `cmp_${Date.now()}`,
        recipientEmail: delivery.recipient,
        subject: emailPayload.subject,
        summary,
        status: "sent",
        messageId: delivery.messageId || "",
        errorMessage: "",
        source: "cloud-function",
        isRead: false
      },
      logger
    );

    return {
      sent: true,
      type: "companyBooking",
      docId: requestId,
      recipient: delivery.recipient,
      messageId: delivery.messageId,
      subject: emailPayload.subject
    };
  } catch (err) {
    const safeError = sanitizeSmtpErrorMessage(err, smtpConfig.pass);
    await safeRecordAlertLog(
      recordAlertLog,
      {
        type: "company",
        recordId: requestId || `cmp_${Date.now()}`,
        recipientEmail: smtpConfig.adminEmail,
        subject: emailPayload.subject,
        summary,
        status: "failed",
        messageId: "",
        errorMessage: safeError,
        source: "cloud-function",
        isRead: false
      },
      logger
    );
    throw err;
  }
}

const TEST_EMAIL_SAMPLE_RECORDS = Object.freeze({
  booking: Object.freeze({
    clientName: "Simranpreet Kaur",
    contactNumber: "+91 9812890081",
    serviceType: "High-Fashion Editorial — Pro Package",
    preferredDate: "2026-12-14",
    preferredTime: "11:00 AM",
    location: "Star Artist Studio Floor (Talwara Khurd, Sirsa)",
    status: "Pending",
    notes: "SMTP & HTML template test booking preview from the Admin Console."
  }),
  model: Object.freeze({
    fullName: "Aarav Sharma",
    age: 22,
    gender: "Male",
    city: "Sirsa, Haryana",
    phoneNumber: "+91 9812890081",
    skills: "Editorial Print, Ramp Walk, Ethnic & Bridal Couture",
    portfolioUrls: "https://gen-lang-client-0548683163.web.app/#gallery",
    consentGiven: true,
    status: "pending"
  }),
  company: Object.freeze({
    companyName: "Royal Heritage Couture Events",
    organizationType: "Event Organizer",
    contactPerson: "Vikram Malhotra",
    mobileNumber: "+91 9812890081",
    email: "casting@royalheritageevents.in",
    address: "Hissar Road Commercial Complex, Sirsa, Haryana",
    modelRequirement: "Both",
    modelsCount: 6,
    eventType: "Fashion Show",
    eventDate: "2026-12-22",
    eventLocation: "Grand Courtyard Banquet, Sirsa",
    budget: "₹85,000",
    requestedModelNames: ["Ananya Verma", "Kabir Gill"],
    detailedRequirements: "SMTP & HTML template test company booking preview from the Admin Console.",
    status: "Pending"
  })
});

function sanitizeSmtpErrorMessage(err, smtpPass = "") {
  let msg = String(err?.message || err || "SMTP delivery failed.").trim();
  if (smtpPass && smtpPass.length >= 4) {
    msg = msg.split(smtpPass).join("[REDACTED]");
  }
  return msg.slice(0, 280);
}

/**
 * Core handler invoked when the authorized Studio Admin queues a test notification
 * email in adminEmailTests/{testId} from the Admin Dashboard Email Preview tab.
 * Always restricts delivery strictly to the authorized admin email (never open relay).
 */
export async function handleAdminTestEmailRequested(
  event,
  {
    env = process.env,
    smtpUser = "",
    smtpPass = "",
    transporter = null,
    verifySmtpImpl = verifySmtpTransport,
    sendMailImpl = sendAdminAlertEmail,
    templateOverrides = null,
    loadTemplateOverrides = null,
    updateStatusDoc = null,
    logger = console
  } = {}
) {
  const payload = event?.data?.data?.();
  const testId = event?.params?.testId || event?.data?.id || "";

  if (!payload || typeof payload !== "object") {
    logger.warn("Skipping test email: empty Firestore document snapshot.", { testId });
    return { sent: false, reason: "empty_snapshot", type: "testEmail", docId: testId };
  }

  const smtpConfig = resolveSmtpConfig(env, { smtpUser, smtpPass });
  const requesterEmail = String(payload.requestedByEmail || "").trim().toLowerCase();
  const authorizedEmail = String(smtpConfig.adminEmail || "").trim().toLowerCase();

  if (!requesterEmail || requesterEmail !== authorizedEmail) {
    const errMsg = "Rejected test email request: unauthorized requester email.";
    logger.warn(errMsg, { testId, requesterEmail });
    if (typeof updateStatusDoc === "function") {
      await updateStatusDoc({
        status: "failed",
        smtpVerified: false,
        errorMessage: errMsg,
        processedAt: new Date().toISOString()
      });
    }
    return { sent: false, reason: "unauthorized_requester", type: "testEmail", docId: testId };
  }

  const templateType = ["booking", "model", "company"].includes(payload.templateType)
    ? payload.templateType
    : "booking";

  const persistedOverrides = await resolveRuntimeOverrides({
    templateOverrides,
    loadTemplateOverrides,
    logger
  });
  const requestOverrides =
    payload.templateOverrides && typeof payload.templateOverrides === "object"
      ? payload.templateOverrides
      : {};
  const mergedOverrides = { ...persistedOverrides, ...requestOverrides };

  const sourceRecord =
    payload.recordSnapshot && typeof payload.recordSnapshot === "object" && Object.keys(payload.recordSnapshot).length > 0
      ? payload.recordSnapshot
      : TEST_EMAIL_SAMPLE_RECORDS[templateType];
  const recordId = String(payload.recordId || testId || `test_${templateType}`).trim().slice(0, 128);

  let emailPayload;
  if (templateType === "model") {
    emailPayload = buildNewModelApplicationAlertEmail({
      modelId: recordId,
      model: sourceRecord,
      dashboardUrl: smtpConfig.dashboardUrl,
      env,
      templateOverrides: mergedOverrides
    });
  } else if (templateType === "company") {
    emailPayload = buildNewCompanyBookingAlertEmail({
      requestId: recordId,
      request: sourceRecord,
      dashboardUrl: smtpConfig.dashboardUrl,
      env,
      templateOverrides: mergedOverrides
    });
  } else {
    emailPayload = buildNewBookingAlertEmail({
      bookingId: recordId,
      booking: sourceRecord,
      dashboardUrl: smtpConfig.dashboardUrl,
      env,
      templateOverrides: mergedOverrides
    });
  }

  const testSubject = emailPayload.subject.startsWith("[TEST]")
    ? emailPayload.subject
    : `[TEST] ${emailPayload.subject}`;

  try {
    const verification = await verifySmtpImpl({ smtpConfig, transporter });
    const verifiedTransporter = verification?.transporter || transporter;
    const delivery = await sendMailImpl({
      ...emailPayload,
      subject: testSubject,
      smtpConfig,
      transporter: verifiedTransporter
    });

    const processedAt = new Date().toISOString();
    if (typeof updateStatusDoc === "function") {
      await updateStatusDoc({
        status: "sent",
        smtpVerified: true,
        smtpHost: smtpConfig.host,
        smtpPort: smtpConfig.port,
        recipientEmail: delivery.recipient,
        renderedSubject: testSubject,
        messageId: delivery.messageId || "",
        errorMessage: "",
        processedAt
      });
    }

    logger.info("Sent admin test notification email.", {
      testId,
      templateType,
      recipient: delivery.recipient,
      messageId: delivery.messageId
    });

    return {
      sent: true,
      smtpVerified: true,
      type: "testEmail",
      templateType,
      docId: testId,
      recipient: delivery.recipient,
      messageId: delivery.messageId,
      subject: testSubject,
      smtpHost: smtpConfig.host,
      smtpPort: smtpConfig.port
    };
  } catch (err) {
    const safeError = sanitizeSmtpErrorMessage(err, smtpConfig.pass);
    logger.error("Admin test notification email failed.", {
      testId,
      templateType,
      error: safeError
    });
    if (typeof updateStatusDoc === "function") {
      await updateStatusDoc({
        status: "failed",
        smtpVerified: false,
        smtpHost: smtpConfig.host,
        smtpPort: smtpConfig.port,
        recipientEmail: smtpConfig.adminEmail,
        renderedSubject: testSubject,
        errorMessage: safeError,
        processedAt: new Date().toISOString()
      });
    }
    return {
      sent: false,
      smtpVerified: false,
      reason: "smtp_error",
      errorMessage: safeError,
      type: "testEmail",
      templateType,
      docId: testId,
      recipient: smtpConfig.adminEmail
    };
  }
}
