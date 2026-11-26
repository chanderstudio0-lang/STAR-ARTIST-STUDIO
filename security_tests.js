/**
 * STAR ARTIST STUDIO — Automated Firestore Security Rules Test Suite
 *
 * Verifies against the real Cloud Firestore Rules Emulator that `firestore.rules`
 * strictly blocks unauthorized, anonymous, unverified, and cross-customer access
 * to booking data, user data, private portal catalogs/slots, private chats,
 * and admin-only collections.
 *
 * Run locally with the Firestore Emulator:
 *   firebase emulators:exec --only firestore --project demo-star-artist "node --test security_tests.js"
 */

"use strict";

const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const path = require("node:path");

const PROJECT_ID = process.env.GCP_PROJECT || "demo-star-artist";
const CUSTOMER_A_UID = "customer_a_uid_001";
const CUSTOMER_B_UID = "customer_b_uid_002";
const ADMIN_UID = "authorized_admin_uid";
const OTHER_GMAIL_UID = "other_gmail_uid_003";
const SPOOF_ADMIN_UID = "spoof_unverified_admin_uid";
const ANON_UID = "anonymous_guest_uid";

const AUTHORIZED_ADMIN_EMAIL = "chanderstudio0@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

let testEnv;

function getCustomerAContext() {
  return testEnv.authenticatedContext(CUSTOMER_A_UID, {
    email: "customer.a@gmail.com",
    email_verified: true,
    firebase: { sign_in_provider: "password" },
  });
}

function getCustomerBContext() {
  return testEnv.authenticatedContext(CUSTOMER_B_UID, {
    email: "customer.b@gmail.com",
    email_verified: true,
    firebase: { sign_in_provider: "password" },
  });
}

function getAdminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: AUTHORIZED_ADMIN_EMAIL,
    email_verified: true,
    firebase: { sign_in_provider: "google.com" },
  });
}

function getUnverifiedAdminContext() {
  return testEnv.authenticatedContext(SPOOF_ADMIN_UID, {
    email: AUTHORIZED_ADMIN_EMAIL,
    email_verified: false,
    firebase: { sign_in_provider: "password" },
  });
}

function getOtherGmailContext() {
  return testEnv.authenticatedContext(OTHER_GMAIL_UID, {
    email: "someone.else@gmail.com",
    email_verified: true,
    firebase: { sign_in_provider: "google.com" },
  });
}

function getAnonymousContext() {
  return testEnv.authenticatedContext(ANON_UID, {
    firebase: { sign_in_provider: "anonymous" },
  });
}

function pastDate(msAgo = 5000) {
  return new Date(Date.now() - msAgo);
}

before(async () => {
  const rulesPath = path.resolve(__dirname, "firestore.rules");
  const rules = fs.readFileSync(rulesPath, "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// ============================================================================
// 1. UNAUTHENTICATED & ANONYMOUS ACCESS BLOCKING
// ============================================================================

test("1. Unauthenticated visitors are completely blocked from reading or writing bookings, users, and private portal data", async () => {
  const createdAt = pastDate();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(CUSTOMER_A_UID).set({
      userId: CUSTOMER_A_UID,
      email: "customer.a@gmail.com",
      displayName: "Aarav Sharma",
      phoneNumber: "+91 9811100001",
      role: "user",
      createdAt,
      updatedAt: createdAt,
    });
    await db.collection("bookings").doc("booking_a1").set({
      userId: CUSTOMER_A_UID,
      clientName: "Aarav Sharma",
      contactNumber: "+91 9811100001",
      serviceType: "Wedding Photography",
      preferredDate: "2026-12-10",
      preferredTime: "10:00 AM",
      location: "Sirsa Palace",
      status: "Pending",
      createdAt,
      updatedAt: createdAt,
    });
    await db.collection("customerCatalogs").doc("cat_a1").set({
      customerUid: CUSTOMER_A_UID,
      bookingId: "booking_a1",
      title: "Private Wedding Lookbook",
      sharedByAdminEmail: AUTHORIZED_ADMIN_EMAIL,
      status: "active",
      createdAt,
      updatedAt: createdAt,
    });
    await db.collection("customerSlots").doc("slot_a1").set({
      customerUid: CUSTOMER_A_UID,
      bookingId: "booking_a1",
      eventTitle: "Wedding Morning Shoot",
      eventDate: "2026-12-10",
      eventTime: "10:00 AM - 02:00 PM",
      slotLabel: "Slot A - Studio Bay 1",
      approvalStatus: "Confirmed",
      assignedByAdminEmail: AUTHORIZED_ADMIN_EMAIL,
      createdAt,
      updatedAt: createdAt,
    });
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();

  // Read (get & list) blocked on all protected collections
  await assertFails(unauthDb.collection("bookings").doc("booking_a1").get());
  await assertFails(unauthDb.collection("bookings").get());
  await assertFails(unauthDb.collection("users").doc(CUSTOMER_A_UID).get());
  await assertFails(unauthDb.collection("users").get());
  await assertFails(unauthDb.collection("customerCatalogs").doc("cat_a1").get());
  await assertFails(unauthDb.collection("customerCatalogs").get());
  await assertFails(unauthDb.collection("customerSlots").doc("slot_a1").get());
  await assertFails(unauthDb.collection("customerSlots").get());
  await assertFails(unauthDb.collection("privateChats").get());
  await assertFails(unauthDb.collection("admins").get());
  await assertFails(unauthDb.collection("studioConfig").get());

  // Unauthenticated writes blocked
  await assertFails(
    unauthDb.collection("bookings").doc("unauth_booking").set({
      userId: CUSTOMER_A_UID,
      clientName: "Guest User",
      contactNumber: "9812890081",
      serviceType: "Photography",
      preferredDate: "2026-12-15",
      status: "Pending",
      createdAt: pastDate(1000),
    })
  );
  await assertFails(
    unauthDb.collection("users").doc("unauth_user").set({
      userId: "unauth_user",
      email: "guest@example.com",
      displayName: "Guest",
      role: "user",
      createdAt: pastDate(1000),
    })
  );
});

test("2. Anonymous Firebase Auth sessions are denied access to bookings and user records", async () => {
  const createdAt = pastDate();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("bookings").doc("booking_a1").set({
      userId: CUSTOMER_A_UID,
      clientName: "Aarav Sharma",
      contactNumber: "+91 9811100001",
      serviceType: "Photography",
      preferredDate: "2026-12-10",
      status: "Pending",
      createdAt,
    });
    await db.collection("users").doc(CUSTOMER_A_UID).set({
      userId: CUSTOMER_A_UID,
      email: "customer.a@gmail.com",
      displayName: "Aarav Sharma",
      role: "user",
      createdAt,
    });
  });

  const anonDb = getAnonymousContext().firestore();

  await assertFails(anonDb.collection("bookings").doc("booking_a1").get());
  await assertFails(
    anonDb.collection("bookings").where("userId", "==", ANON_UID).get()
  );
  await assertFails(anonDb.collection("users").doc(CUSTOMER_A_UID).get());
  await assertFails(
    anonDb.collection("users").doc(ANON_UID).set({
      userId: ANON_UID,
      email: "",
      displayName: "Anon",
      role: "user",
      createdAt: pastDate(1000),
    })
  );
  await assertFails(
    anonDb.collection("bookings").doc("anon_booking").set({
      userId: ANON_UID,
      clientName: "Anonymous Guest",
      contactNumber: "9812890081",
      serviceType: "Photography",
      preferredDate: "2026-12-20",
      status: "Pending",
      createdAt: pastDate(1000),
    })
  );
});

// ============================================================================
// 2. USER PROFILE & SUBCOLLECTION PRIVACY ISOLATION
// ============================================================================

test("3. User data isolation: customer can manage only their own profile and cannot read, list, modify, or delete another user's profile", async () => {
  const createdAt = pastDate();
  const aliceDb = getCustomerAContext().firestore();
  const bobDb = getCustomerBContext().firestore();

  // Customer A creates their own profile
  await assertSucceeds(
    aliceDb.collection("users").doc(CUSTOMER_A_UID).set({
      userId: CUSTOMER_A_UID,
      email: "customer.a@gmail.com",
      displayName: "Aarav Sharma",
      phoneNumber: "+91 9811100001",
      city: "Sirsa",
      role: "user",
      createdAt,
      updatedAt: createdAt,
    })
  );

  // Customer B creates their own profile
  await assertSucceeds(
    bobDb.collection("users").doc(CUSTOMER_B_UID).set({
      userId: CUSTOMER_B_UID,
      email: "customer.b@gmail.com",
      displayName: "Meera Verma",
      phoneNumber: "+91 9999900002",
      city: "Hisar",
      role: "user",
      createdAt,
      updatedAt: createdAt,
    })
  );

  // Customer A can read and update allowed fields on own profile
  await assertSucceeds(aliceDb.collection("users").doc(CUSTOMER_A_UID).get());
  await assertSucceeds(
    aliceDb.collection("users").doc(CUSTOMER_A_UID).update({
      displayName: "Aarav S.",
      city: "Sirsa, Haryana",
      updatedAt: pastDate(1000),
    })
  );

  // Cross-customer profile get, list, update, and delete are strictly denied
  await assertFails(aliceDb.collection("users").doc(CUSTOMER_B_UID).get());
  await assertFails(aliceDb.collection("users").get());
  await assertFails(
    aliceDb.collection("users").where("userId", "==", CUSTOMER_B_UID).get()
  );
  await assertFails(
    aliceDb.collection("users").doc(CUSTOMER_B_UID).update({
      displayName: "Tampered Name",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(aliceDb.collection("users").doc(CUSTOMER_B_UID).delete());

  // Customer A cannot impersonate Customer B on profile creation
  await assertFails(
    aliceDb.collection("users").doc("spoof_uid_999").set({
      userId: CUSTOMER_A_UID,
      email: "spoof@gmail.com",
      displayName: "Spoof",
      role: "user",
      createdAt: pastDate(1000),
    })
  );
});

test("4. User privilege & subcollection protection: customer cannot escalate role to admin or access another user's notifications/history", async () => {
  const createdAt = pastDate();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(CUSTOMER_A_UID).set({
      userId: CUSTOMER_A_UID,
      email: "customer.a@gmail.com",
      displayName: "Aarav Sharma",
      role: "user",
      createdAt,
      updatedAt: createdAt,
    });
    await db
      .collection("users")
      .doc(CUSTOMER_B_UID)
      .collection("notifications")
      .doc("notif_b1")
      .set({
        userId: CUSTOMER_B_UID,
        bookingId: "booking_b1",
        title: "Booking Confirmed",
        message: "Your private shoot is confirmed.",
        status: "Confirmed",
        isRead: false,
        createdAt,
        updatedAt: createdAt,
      });
    await db
      .collection("users")
      .doc(CUSTOMER_B_UID)
      .collection("history")
      .doc("hist_b1")
      .set({
        userId: CUSTOMER_B_UID,
        title: "Private Draft",
        createdAt,
      });
  });

  const aliceDb = getCustomerAContext().firestore();

  // Role self-escalation blocked on update & create
  await assertFails(
    aliceDb.collection("users").doc(CUSTOMER_A_UID).update({
      role: "admin",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    getOtherGmailContext().firestore().collection("users").doc(OTHER_GMAIL_UID).set({
      userId: OTHER_GMAIL_UID,
      email: "someone.else@gmail.com",
      displayName: "Impostor",
      role: "admin",
      createdAt: pastDate(1000),
    })
  );

  // Immutable createdAt / userId tampering blocked
  await assertFails(
    aliceDb.collection("users").doc(CUSTOMER_A_UID).update({
      userId: CUSTOMER_B_UID,
      updatedAt: pastDate(1000),
    })
  );

  // Cross-user subcollection reads/writes blocked
  await assertFails(
    aliceDb
      .collection("users")
      .doc(CUSTOMER_B_UID)
      .collection("notifications")
      .doc("notif_b1")
      .get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(CUSTOMER_B_UID)
      .collection("notifications")
      .get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(CUSTOMER_B_UID)
      .collection("history")
      .doc("hist_b1")
      .get()
  );
});

// ============================================================================
// 3. BOOKING DATA ISOLATION & INTEGRITY
// ============================================================================

test("5. Booking isolation: customer can read & list ONLY own bookings and is blocked from reading, listing, modifying, or deleting another customer's bookings", async () => {
  const createdAt = pastDate();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("bookings").doc("booking_a1").set({
      userId: CUSTOMER_A_UID,
      clientName: "Aarav Sharma",
      contactNumber: "+91 9811100001",
      serviceType: "Wedding Photography",
      preferredDate: "2026-12-10",
      preferredTime: "10:00 AM",
      location: "Sirsa",
      status: "Pending",
      createdAt,
      updatedAt: createdAt,
    });
    await db.collection("bookings").doc("booking_b1").set({
      userId: CUSTOMER_B_UID,
      clientName: "Meera Verma",
      contactNumber: "+91 9999900002",
      serviceType: "Bridal Shoot",
      preferredDate: "2026-12-18",
      preferredTime: "04:00 PM",
      location: "Hisar",
      status: "Confirmed",
      createdAt,
      updatedAt: createdAt,
    });
  });

  const aliceDb = getCustomerAContext().firestore();

  // Allowed: Customer A reads own booking & lists filtered by own UID
  await assertSucceeds(aliceDb.collection("bookings").doc("booking_a1").get());
  await assertSucceeds(
    aliceDb.collection("bookings").where("userId", "==", CUSTOMER_A_UID).get()
  );

  // Denied: Customer A attempts direct document ID tampering for Customer B's booking
  await assertFails(aliceDb.collection("bookings").doc("booking_b1").get());

  // Denied: Unfiltered collection list or querying Customer B's UID
  await assertFails(aliceDb.collection("bookings").get());
  await assertFails(
    aliceDb.collection("bookings").where("userId", "==", CUSTOMER_B_UID).get()
  );

  // Denied: Updating or deleting Customer B's booking
  await assertFails(
    aliceDb.collection("bookings").doc("booking_b1").update({
      status: "Cancelled",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(aliceDb.collection("bookings").doc("booking_b1").delete());

  // Denied: Creating a booking under Customer B's UID
  await assertFails(
    aliceDb.collection("bookings").doc("forged_owner_booking").set({
      userId: CUSTOMER_B_UID,
      clientName: "Forged Owner",
      contactNumber: "9812890081",
      serviceType: "Photography",
      preferredDate: "2026-12-22",
      status: "Pending",
      createdAt: pastDate(1000),
    })
  );
});

test("6. Booking workflow integrity: customer can create Pending booking and cancel own booking, but CANNOT self-approve, self-confirm, or self-complete", async () => {
  const aliceDb = getCustomerAContext().firestore();
  const createdAt = pastDate();

  // Valid Pending booking creation succeeds
  await assertSucceeds(
    aliceDb.collection("bookings").doc("booking_a_valid").set({
      userId: CUSTOMER_A_UID,
      clientName: "Aarav Sharma",
      contactNumber: "+91 9811100001",
      serviceType: "Pre-Wedding Film",
      preferredDate: "2026-12-24",
      preferredTime: "09:00 AM",
      location: "Talwara Khurd Studio",
      status: "Pending",
      createdAt,
      updatedAt: createdAt,
    })
  );

  // Forging Approved or Confirmed on creation fails
  for (const forbiddenStatus of ["Approved", "Confirmed", "Completed", "Rejected"]) {
    await assertFails(
      aliceDb.collection("bookings").doc(`forged_${forbiddenStatus}`).set({
        userId: CUSTOMER_A_UID,
        clientName: "Aarav Sharma",
        contactNumber: "+91 9811100001",
        serviceType: "Photography",
        preferredDate: "2026-12-25",
        status: forbiddenStatus,
        createdAt: pastDate(1000),
      })
    );
  }

  // Self-approving or self-confirming on update fails
  await assertFails(
    aliceDb.collection("bookings").doc("booking_a_valid").update({
      status: "Approved",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    aliceDb.collection("bookings").doc("booking_a_valid").update({
      status: "Confirmed",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    aliceDb.collection("bookings").doc("booking_a_valid").update({
      status: "Completed",
      updatedAt: pastDate(1000),
    })
  );

  // Cancelling own booking succeeds
  await assertSucceeds(
    aliceDb.collection("bookings").doc("booking_a_valid").update({
      status: "Cancelled",
      updatedAt: pastDate(1000),
    })
  );
});

// ============================================================================
// 4. CUSTOMER PRIVATE PORTAL: CATALOGS & BOOKING SLOTS
// ============================================================================

test("7. Private Customer Catalogs & Slots: strictly UID-locked reads and Admin-only writes", async () => {
  const adminDb = getAdminContext().firestore();
  const aliceDb = getCustomerAContext().firestore();
  const bobDb = getCustomerBContext().firestore();
  const createdAt = pastDate();

  // Authorized Admin assigns private catalogs & slots to Customer A and Customer B
  await assertSucceeds(
    adminDb.collection("customerCatalogs").doc("cat_a1").set({
      customerUid: CUSTOMER_A_UID,
      bookingId: "booking_a1",
      title: "Aarav Private Wedding Album",
      description: "Curated bridal & reception shortlist",
      category: "Wedding",
      items: [{ label: "Portrait Set 1", url: "https://example.com/a1" }],
      sharedByAdminEmail: AUTHORIZED_ADMIN_EMAIL,
      status: "active",
      createdAt,
      updatedAt: createdAt,
    })
  );
  await assertSucceeds(
    adminDb.collection("customerCatalogs").doc("cat_b1").set({
      customerUid: CUSTOMER_B_UID,
      bookingId: "booking_b1",
      title: "Meera Confidential Catalog",
      description: "Private portfolio selection",
      category: "Portfolio",
      sharedByAdminEmail: AUTHORIZED_ADMIN_EMAIL,
      status: "active",
      createdAt,
      updatedAt: createdAt,
    })
  );
  await assertSucceeds(
    adminDb.collection("customerSlots").doc("slot_a1").set({
      customerUid: CUSTOMER_A_UID,
      bookingId: "booking_a1",
      eventTitle: "Aarav Engagement Slot",
      eventDate: "2026-12-10",
      eventTime: "10:00 AM",
      slotLabel: "Morning Bay 1",
      location: "Star Artist Studio",
      approvalStatus: "Approved",
      assignedByAdminEmail: AUTHORIZED_ADMIN_EMAIL,
      createdAt,
      updatedAt: createdAt,
    })
  );
  await assertSucceeds(
    adminDb.collection("customerSlots").doc("slot_b1").set({
      customerUid: CUSTOMER_B_UID,
      bookingId: "booking_b1",
      eventTitle: "Meera Secret Shoot Slot",
      eventDate: "2026-12-18",
      eventTime: "04:00 PM",
      slotLabel: "Evening Bay 2",
      location: "Hisar",
      approvalStatus: "Confirmed",
      assignedByAdminEmail: AUTHORIZED_ADMIN_EMAIL,
      createdAt,
      updatedAt: createdAt,
    })
  );

  // Customer A can read & list strictly their own catalog and slot
  await assertSucceeds(aliceDb.collection("customerCatalogs").doc("cat_a1").get());
  await assertSucceeds(
    aliceDb
      .collection("customerCatalogs")
      .where("customerUid", "==", CUSTOMER_A_UID)
      .get()
  );
  await assertSucceeds(aliceDb.collection("customerSlots").doc("slot_a1").get());
  await assertSucceeds(
    aliceDb
      .collection("customerSlots")
      .where("customerUid", "==", CUSTOMER_A_UID)
      .get()
  );

  // Customer A CANNOT read Customer B's catalog or slot or list all docs
  await assertFails(aliceDb.collection("customerCatalogs").doc("cat_b1").get());
  await assertFails(aliceDb.collection("customerCatalogs").get());
  await assertFails(aliceDb.collection("customerSlots").doc("slot_b1").get());
  await assertFails(aliceDb.collection("customerSlots").get());

  // Customer B CANNOT read Customer A's catalog or slot
  await assertFails(bobDb.collection("customerCatalogs").doc("cat_a1").get());
  await assertFails(bobDb.collection("customerSlots").doc("slot_a1").get());

  // Regular customers CANNOT create, update, or delete catalogs or slots (even own)
  await assertFails(
    aliceDb.collection("customerCatalogs").doc("cat_a_forged").set({
      customerUid: CUSTOMER_A_UID,
      title: "Self Assigned Catalog",
      sharedByAdminEmail: AUTHORIZED_ADMIN_EMAIL,
      status: "active",
      createdAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    aliceDb.collection("customerSlots").doc("slot_a1").update({
      approvalStatus: "Confirmed",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(aliceDb.collection("customerSlots").doc("slot_a1").delete());
});

// ============================================================================
// 5. PRIVATE MESSAGES & CROSS-CUSTOMER HIRING/COMPANY BOOKING ISOLATION
// ============================================================================

test("8. Private Chat isolation: customer sees ONLY own Admin conversation and cannot read or inject into another customer's chat", async () => {
  const createdAt = pastDate();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("privateChats").doc("chat_a1").set({
      participantUid: CUSTOMER_A_UID,
      participantName: "Aarav Sharma",
      participantEmail: "customer.a@gmail.com",
      participantRole: "customer",
      contextType: "general",
      contextLabel: "Customer Portal Support",
      lastMessageText: "Hello Admin",
      lastMessageAt: createdAt,
      lastSenderRole: "customer",
      unreadByAdmin: 1,
      unreadByUser: 0,
      createdAt,
      updatedAt: createdAt,
    });
    await db.collection("privateChats").doc("chat_b1").set({
      participantUid: CUSTOMER_B_UID,
      participantName: "Meera Verma",
      participantEmail: "customer.b@gmail.com",
      participantRole: "customer",
      contextType: "general",
      contextLabel: "Bridal Booking Query",
      lastMessageText: "Confidential venue address",
      lastMessageAt: createdAt,
      lastSenderRole: "customer",
      unreadByAdmin: 1,
      unreadByUser: 0,
      createdAt,
      updatedAt: createdAt,
    });
    await db
      .collection("privateChats")
      .doc("chat_b1")
      .collection("messages")
      .doc("msg_b1")
      .set({
        chatId: "chat_b1",
        senderUid: CUSTOMER_B_UID,
        senderName: "Meera Verma",
        senderRole: "customer",
        text: "Private contact & address details",
        createdAt,
      });
  });

  const aliceDb = getCustomerAContext().firestore();

  // Allowed: Customer A reads own thread
  await assertSucceeds(aliceDb.collection("privateChats").doc("chat_a1").get());
  await assertSucceeds(
    aliceDb
      .collection("privateChats")
      .where("participantUid", "==", CUSTOMER_A_UID)
      .get()
  );

  // Denied: Customer A reading/listing Customer B's chat or messages
  await assertFails(aliceDb.collection("privateChats").doc("chat_b1").get());
  await assertFails(aliceDb.collection("privateChats").get());
  await assertFails(
    aliceDb
      .collection("privateChats")
      .doc("chat_b1")
      .collection("messages")
      .doc("msg_b1")
      .get()
  );
  await assertFails(
    aliceDb.collection("privateChats").doc("chat_b1").collection("messages").get()
  );

  // Denied: Customer A injecting a message into Customer B's private thread
  await assertFails(
    aliceDb
      .collection("privateChats")
      .doc("chat_b1")
      .collection("messages")
      .doc("msg_inject")
      .set({
        chatId: "chat_b1",
        senderUid: CUSTOMER_A_UID,
        senderName: "Aarav Sharma",
        senderRole: "customer",
        text: "Unauthorized injected message",
        createdAt: pastDate(1000),
      })
  );
});

test("9. Hiring & Company Bookings: strict cross-customer isolation and no customer auto-confirmation", async () => {
  const createdAt = pastDate();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("professionalHiringRequests").doc("hire_b1").set({
      userId: CUSTOMER_B_UID,
      customerName: "Meera Verma",
      contactNumber: "+91 9999900002",
      eventType: "Wedding",
      requiredService: "Both",
      eventDate: "2026-12-20",
      eventTime: "06:00 PM",
      eventLocation: "Hisar Grand Resort",
      workingDuration: "2 days",
      budgetRange: "₹45,000 - ₹60,000",
      status: "Pending Admin Review",
      customerAcceptance: "pending",
      createdAt,
      updatedAt: createdAt,
    });
    await db.collection("companyBookings").doc("comp_b1").set({
      userId: CUSTOMER_B_UID,
      companyName: "Meera Couture Pvt Ltd",
      organizationType: "Company",
      contactPerson: "Meera Verma",
      mobileNumber: "+91 9999900002",
      email: "meera@couture.in",
      address: "Sector 14, Hisar",
      modelRequirement: "Female Models",
      modelsCount: 4,
      eventType: "Fashion Show",
      eventDate: "2026-12-22",
      eventLocation: "Hisar Convention Hall",
      budget: "₹80,000",
      detailedRequirements: "Four runway models needed for winter festive showcase.",
      status: "Pending",
      createdAt,
      updatedAt: createdAt,
    });
  });

  const aliceDb = getCustomerAContext().firestore();
  const bobDb = getCustomerBContext().firestore();

  // Cross-customer reads & lists blocked
  await assertFails(
    aliceDb.collection("professionalHiringRequests").doc("hire_b1").get()
  );
  await assertFails(aliceDb.collection("professionalHiringRequests").get());
  await assertFails(aliceDb.collection("companyBookings").doc("comp_b1").get());
  await assertFails(aliceDb.collection("companyBookings").get());

  // Customer B cannot auto-confirm their own hiring request
  await assertFails(
    bobDb.collection("professionalHiringRequests").doc("hire_b1").update({
      status: "Confirmed",
      updatedAt: pastDate(1000),
    })
  );
});

// ============================================================================
// 6. STRICT ADMIN-ONLY AUTHORIZATION & DEFAULT-DENY ENFORCEMENT
// ============================================================================

test("10. Admin authorization: ONLY verified chanderstudio0@gmail.com can access admin data & manage all bookings; spoofed/other Gmail accounts and unknown collections are denied", async () => {
  const createdAt = pastDate();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("bookings").doc("booking_a1").set({
      userId: CUSTOMER_A_UID,
      clientName: "Aarav Sharma",
      contactNumber: "+91 9811100001",
      serviceType: "Wedding Photography",
      preferredDate: "2026-12-10",
      status: "Pending",
      createdAt,
      updatedAt: createdAt,
    });
    await db.collection("studioConfig").doc("emailAlerts").set({
      bookingAlertsEnabled: true,
      modelAlertsEnabled: true,
      recipientEmail: AUTHORIZED_ADMIN_EMAIL,
      updatedAt: createdAt,
    });
    await db.collection("adminEmailAlerts").doc("alert_1").set({
      type: "booking",
      recordId: "booking_a1",
      recipientEmail: AUTHORIZED_ADMIN_EMAIL,
      subject: "New Booking Alert",
      status: "sent",
      createdAt,
      updatedAt: createdAt,
    });
    await db.collection("admins").doc(OTHER_GMAIL_UID).set({
      email: "someone.else@gmail.com",
      role: "admin",
    });
  });

  const adminDb = getAdminContext().firestore();
  const unverifiedAdminDb = getUnverifiedAdminContext().firestore();
  const otherGmailDb = getOtherGmailContext().firestore();
  const aliceDb = getCustomerAContext().firestore();

  // Authorized Admin CAN list all bookings, approve/confirm them, and read admin config/logs
  await assertSucceeds(adminDb.collection("bookings").get());
  await assertSucceeds(
    adminDb.collection("bookings").doc("booking_a1").update({
      status: "Confirmed",
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(adminDb.collection("studioConfig").doc("emailAlerts").get());
  await assertSucceeds(adminDb.collection("adminEmailAlerts").get());

  // Unverified admin email token is DENIED everything admin-only
  await assertFails(unverifiedAdminDb.collection("bookings").get());
  await assertFails(
    unverifiedAdminDb.collection("bookings").doc("booking_a1").update({
      status: "Rejected",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    unverifiedAdminDb.collection("studioConfig").doc("emailAlerts").get()
  );
  await assertFails(unverifiedAdminDb.collection("adminEmailAlerts").get());

  // Another verified Gmail account (even if present in /admins) is DENIED
  await assertFails(otherGmailDb.collection("admins").doc(OTHER_GMAIL_UID).get());
  await assertFails(otherGmailDb.collection("bookings").doc("booking_a1").get());
  await assertFails(
    otherGmailDb.collection("studioConfig").doc("emailAlerts").get()
  );
  await assertFails(otherGmailDb.collection("adminEmailAlerts").get());

  // Regular customer is DENIED admin collections & unknown collections (default-deny)
  await assertFails(aliceDb.collection("admins").get());
  await assertFails(aliceDb.collection("studioConfig").get());
  await assertFails(aliceDb.collection("adminEmailAlerts").get());
  await assertFails(aliceDb.collection("internalReports").get());
  await assertFails(
    aliceDb.collection("internalReports").doc("secret_1").set({ total: 99 })
  );
});
