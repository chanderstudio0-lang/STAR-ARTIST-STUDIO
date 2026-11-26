import { getApps, initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { defineSecret } from "firebase-functions/params";
import * as logger from "firebase-functions/logger";
import { FIRESTORE_DATABASE_ID, FUNCTIONS_REGION } from "./src/config.js";
import {
  handleNewBookingCreated,
  handleNewModelApplicationCreated,
  handleNewCompanyBookingCreated,
  handleAdminTestEmailRequested
} from "./src/handlers.js";

if (!getApps().length) {
  initializeApp();
}

const SMTP_USER = defineSecret("SMTP_USER");
const SMTP_PASS = defineSecret("SMTP_PASS");

/**
 * Optionally loads live email branding/template overrides from Firestore
 * (`studioConfig/emailTemplates` in the named database). Falls back silently to defaults.
 */
async function loadFirestoreEmailTemplateOverrides() {
  try {
    const db = getFirestore(FIRESTORE_DATABASE_ID);
    const snap = await db.doc("studioConfig/emailTemplates").get();
    return snap.exists ? snap.data() || {} : {};
  } catch (err) {
    logger.warn("Unable to read studioConfig/emailTemplates; using built-in HTML templates.", {
      error: err?.message || String(err)
    });
    return {};
  }
}

/**
 * Loads Admin Dashboard automated email alert toggle settings from Firestore
 * (`studioConfig/emailAlerts`). Defaults to enabled when unset.
 */
async function loadFirestoreEmailAlertSettings() {
  try {
    const db = getFirestore(FIRESTORE_DATABASE_ID);
    const snap = await db.doc("studioConfig/emailAlerts").get();
    return snap.exists ? snap.data() || {} : {};
  } catch (err) {
    logger.warn("Unable to read studioConfig/emailAlerts; defaulting to alerts enabled.", {
      error: err?.message || String(err)
    });
    return {};
  }
}

/**
 * Writes automated alert delivery status entries into `adminEmailAlerts/{alertId}`
 * so the Admin Dashboard displays real-time delivery history.
 */
async function writeAdminEmailAlertLog(entry) {
  const db = getFirestore(FIRESTORE_DATABASE_ID);
  const safeType = ["booking", "model", "company"].includes(entry?.type) ? entry.type : "booking";
  const safeRecordId = String(entry?.recordId || "").replace(/[^a-zA-Z0-9_-]/g, "").slice(0, 64) || "item";
  const alertId = `alert_${safeType}_${safeRecordId}_${Date.now()}`.slice(0, 120);
  await db.doc(`adminEmailAlerts/${alertId}`).set({
    type: safeType,
    recordId: String(entry?.recordId || safeRecordId).slice(0, 128),
    recipientEmail: String(entry?.recipientEmail || "chanderstudio0@gmail.com").trim().toLowerCase(),
    subject: String(entry?.subject || "Studio Admin Alert").slice(0, 200),
    summary: String(entry?.summary || "").slice(0, 500),
    status: ["queued", "sent", "failed", "skipped", "acknowledged"].includes(entry?.status) ? entry.status : "sent",
    messageId: String(entry?.messageId || "").slice(0, 200),
    errorMessage: String(entry?.errorMessage || "").slice(0, 300),
    source: "cloud-function",
    isRead: false,
    createdAt: FieldValue.serverTimestamp(),
    updatedAt: FieldValue.serverTimestamp()
  });
}

/**
 * Fires automatically when a new shoot booking document is created in Firestore
 * and sends a branded HTML alert to the authorized Studio Administrator (chanderstudio0@gmail.com).
 */
export const notifyAdminOnNewBooking = onDocumentCreated(
  {
    document: "bookings/{bookingId}",
    database: FIRESTORE_DATABASE_ID,
    region: FUNCTIONS_REGION,
    secrets: [SMTP_USER, SMTP_PASS],
    retry: false
  },
  async (event) => {
    await handleNewBookingCreated(event, {
      smtpUser: SMTP_USER.value(),
      smtpPass: SMTP_PASS.value(),
      loadTemplateOverrides: loadFirestoreEmailTemplateOverrides,
      loadAlertSettings: loadFirestoreEmailAlertSettings,
      recordAlertLog: writeAdminEmailAlertLog,
      logger
    });
  }
);

/**
 * Fires automatically when a new 18+ model application document is created in Firestore
 * and sends a branded HTML alert to the authorized Studio Administrator (chanderstudio0@gmail.com).
 */
export const notifyAdminOnNewModelApplication = onDocumentCreated(
  {
    document: "models/{modelId}",
    database: FIRESTORE_DATABASE_ID,
    region: FUNCTIONS_REGION,
    secrets: [SMTP_USER, SMTP_PASS],
    retry: false
  },
  async (event) => {
    await handleNewModelApplicationCreated(event, {
      smtpUser: SMTP_USER.value(),
      smtpPass: SMTP_PASS.value(),
      loadTemplateOverrides: loadFirestoreEmailTemplateOverrides,
      loadAlertSettings: loadFirestoreEmailAlertSettings,
      recordAlertLog: writeAdminEmailAlertLog,
      logger
    });
  }
);

/**
 * Fires automatically when a new Company / Event Organizer model requirement booking is created
 * and sends a branded HTML alert to the authorized Studio Administrator (chanderstudio0@gmail.com).
 */
export const notifyAdminOnNewCompanyBooking = onDocumentCreated(
  {
    document: "companyBookings/{requestId}",
    database: FIRESTORE_DATABASE_ID,
    region: FUNCTIONS_REGION,
    secrets: [SMTP_USER, SMTP_PASS],
    retry: false
  },
  async (event) => {
    await handleNewCompanyBookingCreated(event, {
      smtpUser: SMTP_USER.value(),
      smtpPass: SMTP_PASS.value(),
      loadTemplateOverrides: loadFirestoreEmailTemplateOverrides,
      loadAlertSettings: loadFirestoreEmailAlertSettings,
      recordAlertLog: writeAdminEmailAlertLog,
      logger
    });
  }
);

/**
 * Fires when the verified Studio Admin triggers "Send Test Email" from the
 * Admin Dashboard Email Preview panel. Verifies SMTP and sends a [TEST] HTML
 * email strictly to chanderstudio0@gmail.com, then writes delivery telemetry back.
 */
export const sendAdminTestNotificationEmail = onDocumentCreated(
  {
    document: "adminEmailTests/{testId}",
    database: FIRESTORE_DATABASE_ID,
    region: FUNCTIONS_REGION,
    secrets: [SMTP_USER, SMTP_PASS],
    retry: false
  },
  async (event) => {
    const db = getFirestore(FIRESTORE_DATABASE_ID);
    const testId = event?.params?.testId || event?.data?.id || "";
    const statusRef = testId ? db.doc(`adminEmailTests/${testId}`) : null;

    await handleAdminTestEmailRequested(event, {
      smtpUser: SMTP_USER.value(),
      smtpPass: SMTP_PASS.value(),
      loadTemplateOverrides: loadFirestoreEmailTemplateOverrides,
      updateStatusDoc: async (patch) => {
        if (!statusRef) return;
        try {
          await statusRef.set(patch, { merge: true });
        } catch (err) {
          logger.warn("Unable to update adminEmailTests status document.", {
            testId,
            error: err?.message || String(err)
          });
        }
      },
      logger
    });
  }
);
