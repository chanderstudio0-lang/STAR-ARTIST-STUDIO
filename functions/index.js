import { getApps, initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { defineSecret } from "firebase-functions/params";
import * as logger from "firebase-functions/logger";
import { FIRESTORE_DATABASE_ID, FUNCTIONS_REGION } from "./src/config.js";
import {
  handleNewBookingCreated,
  handleNewModelApplicationCreated,
  handleNewCompanyBookingCreated
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
      logger
    });
  }
);
