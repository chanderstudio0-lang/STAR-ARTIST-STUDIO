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

// ============================================================================
// 7. ADMIN RECIPIENT VIEW TRIGGER: MESSAGE READ RECEIPT UPDATE
// ============================================================================

test("11. Message read receipts: Admin recipient view trigger updates isRead=true on a specific customer message doc in Firestore while non-admins cannot forge read receipts", async () => {
  const assert = require("node:assert/strict");
  const createdAt = pastDate(8000);
  const aliceDb = getCustomerAContext().firestore();
  const bobDb = getCustomerBContext().firestore();
  const adminDb = getAdminContext().firestore();

  // Customer A opens a private chat thread and sends an unread message (isRead: false)
  await assertSucceeds(
    aliceDb.collection("privateChats").doc("chat_receipt_a1").set({
      participantUid: CUSTOMER_A_UID,
      participantName: "Aarav Sharma",
      participantEmail: "customer.a@gmail.com",
      participantRole: "customer",
      contextType: "general",
      contextLabel: "Wedding Shoot Inquiry",
      lastMessageText: "Please confirm my morning slot",
      lastMessageAt: createdAt,
      lastSenderRole: "customer",
      unreadByAdmin: 1,
      unreadByUser: 0,
      createdAt,
      updatedAt: createdAt,
    })
  );

  const msgRefForAlice = aliceDb
    .collection("privateChats")
    .doc("chat_receipt_a1")
    .collection("messages")
    .doc("msg_specific_01");

  await assertSucceeds(
    msgRefForAlice.set({
      chatId: "chat_receipt_a1",
      senderUid: CUSTOMER_A_UID,
      senderName: "Aarav Sharma",
      senderRole: "customer",
      text: "Please confirm my morning slot",
      isRead: false,
      createdAt,
    })
  );

  // Customer A CANNOT forge read status on their own sent message
  await assertFails(
    msgRefForAlice.update({
      isRead: true,
      readAt: pastDate(1000),
      readByUid: CUSTOMER_A_UID,
      readByRole: "customer",
    })
  );

  // Customer B CANNOT update read status on Customer A's message
  await assertFails(
    bobDb
      .collection("privateChats")
      .doc("chat_receipt_a1")
      .collection("messages")
      .doc("msg_specific_01")
      .update({
        isRead: true,
        readAt: pastDate(1000),
        readByUid: CUSTOMER_B_UID,
        readByRole: "customer",
      })
  );

  // Admin opens the thread (recipient view trigger fires) -> updates isRead on that specific message doc
  const msgRefForAdmin = adminDb
    .collection("privateChats")
    .doc("chat_receipt_a1")
    .collection("messages")
    .doc("msg_specific_01");

  const readTimestamp = pastDate(500);
  await assertSucceeds(
    msgRefForAdmin.update({
      isRead: true,
      readAt: readTimestamp,
      readByUid: ADMIN_UID,
      readByRole: "admin",
    })
  );

  // Verify stored Firestore document reflects updated read receipt fields
  const updatedSnap = await assertSucceeds(msgRefForAlice.get());
  const updatedData = updatedSnap.data();
  assert.equal(updatedData.isRead, true);
  assert.equal(updatedData.readByRole, "admin");
  assert.equal(updatedData.readByUid, ADMIN_UID);
  assert.ok(updatedData.readAt);
});

// ============================================================================
// 8. BOOKING SLOT AVAILABILITY & DOUBLE-BOOKING PREVENTION
// ============================================================================

test("12. Studio bookedSlots: signed-in customers can view privacy-safe availability, reserve an open slot ONLY with an atomic Pending booking, and are blocked from double-booking or slot-squatting", async () => {
  const createdAt = pastDate();
  const aliceDb = getCustomerAContext().firestore();
  const bobDb = getCustomerBContext().firestore();
  const adminDb = getAdminContext().firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  const slotKey = "2026-12-20__morning_gold";
  const bookingId = "bk_slot_atomic_a1";

  // Direct slot-squatting WITHOUT a real owned Pending booking is DENIED (Medium #6 fix)
  await assertFails(
    aliceDb.collection("bookedSlots").doc(slotKey).set({
      slotKey,
      date: "2026-12-20",
      slotCode: "morning_gold",
      preferredTime: "10:00 AM",
      status: "reserved",
      createdAt,
      updatedAt: createdAt,
    })
  );
  await assertFails(
    aliceDb.collection("bookedSlots").doc(slotKey).set({
      slotKey,
      date: "2026-12-20",
      slotCode: "morning_gold",
      preferredTime: "10:00 AM",
      status: "reserved",
      bookingId: "non_existent_booking_999",
      createdAt,
      updatedAt: createdAt,
    })
  );

  // Mismatched slotKey != date__slotCode is DENIED
  await assertFails(
    aliceDb.collection("bookedSlots").doc("2026-12-20__wrong_code").set({
      slotKey: "2026-12-20__wrong_code",
      date: "2026-12-20",
      slotCode: "morning_gold",
      preferredTime: "10:00 AM",
      status: "reserved",
      bookingId,
      createdAt,
      updatedAt: createdAt,
    })
  );

  // Customer A reserves an open slot atomically alongside a genuine Pending booking -> SUCCEEDS
  const validBatch = aliceDb.batch();
  validBatch.set(aliceDb.collection("bookings").doc(bookingId), {
    userId: CUSTOMER_A_UID,
    clientName: "Aarav Sharma",
    contactNumber: "+91 9811100001",
    serviceType: "Wedding Photography",
    preferredDate: "2026-12-20",
    preferredTime: "10:00 AM",
    location: "Star Artist Studio",
    status: "Pending",
    createdAt,
    updatedAt: createdAt,
  });
  validBatch.set(aliceDb.collection("bookedSlots").doc(slotKey), {
    slotKey,
    date: "2026-12-20",
    slotCode: "morning_gold",
    preferredTime: "10:00 AM",
    status: "reserved",
    bookingId,
    createdAt,
    updatedAt: createdAt,
  });
  await assertSucceeds(validBatch.commit());

  // Customer B can read occupied slots to highlight calendar availability
  await assertSucceeds(bobDb.collection("bookedSlots").get());
  await assertSucceeds(bobDb.collection("bookedSlots").doc(slotKey).get());

  // Unauthenticated visitor CANNOT read bookedSlots
  await assertFails(unauthDb.collection("bookedSlots").get());

  // Double-booking prevention: Customer B CANNOT overwrite/update or delete the already-booked slot
  await assertFails(
    bobDb.collection("bookedSlots").doc(slotKey).set({
      slotKey,
      date: "2026-12-20",
      slotCode: "morning_gold",
      preferredTime: "10:00 AM",
      status: "reserved",
      bookingId: "bk_bob_double",
      createdAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(bobDb.collection("bookedSlots").doc(slotKey).delete());

  // Privacy guard: cannot store customer PII (name/phone/userId) inside bookedSlots
  const piiBatch = aliceDb.batch();
  piiBatch.set(aliceDb.collection("bookings").doc("bk_slot_pii_a2"), {
    userId: CUSTOMER_A_UID,
    clientName: "Aarav Sharma",
    contactNumber: "+91 9811100001",
    serviceType: "Portrait",
    preferredDate: "2026-12-21",
    preferredTime: "11:30 AM",
    status: "Pending",
    createdAt: pastDate(1000),
    updatedAt: pastDate(1000),
  });
  piiBatch.set(aliceDb.collection("bookedSlots").doc("2026-12-21__main_bay"), {
    slotKey: "2026-12-21__main_bay",
    date: "2026-12-21",
    slotCode: "main_bay",
    preferredTime: "11:30 AM",
    status: "reserved",
    bookingId: "bk_slot_pii_a2",
    clientName: "Leak Name",
    createdAt: pastDate(1000),
    updatedAt: pastDate(1000),
  });
  await assertFails(piiBatch.commit());

  // Authorized Admin CAN confirm or release a slot
  await assertSucceeds(
    adminDb.collection("bookedSlots").doc(slotKey).update({
      status: "confirmed",
      updatedAt: pastDate(500),
    })
  );
  await assertSucceeds(adminDb.collection("bookedSlots").doc(slotKey).delete());
});

// ============================================================================
// 9. UPI ADVANCE PAYMENT & WEDDING BOOKING PAYMENT SECURITY RULES
// ============================================================================

test("13. UPI Payment System: official UPI 9812890081@ybl stays disabled until QR is uploaded & verified, ONLY chanderstudio0@gmail.com controls settings/pricing/verification, and strict UID isolation holds", async () => {
  const createdAt = pastDate(6000);
  const aliceDb = getCustomerAContext().firestore();
  const bobDb = getCustomerBContext().firestore();
  const adminDb = getAdminContext().firestore();
  const unverifiedAdminDb = getUnverifiedAdminContext().firestore();
  const otherGmailDb = getOtherGmailContext().firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  const sampleUploadedQr = "data:image/png;base64," + "A".repeat(160);

  // 1a. Admin saves official Studio UPI ID (9812890081@ybl) in DISABLED state before QR upload -> SUCCEEDS
  const disabledOfficialCfg = {
    enabled: false,
    upiId: "9812890081@ybl",
    payeeName: "STAR ARTIST STUDIO",
    qrImageDataUrl: "",
    qrVerified: false,
    instructions: "Pay advance via official Studio UPI and submit 12-digit UTR.",
    modelAdvanceMode: "percentage",
    modelAdvanceValue: 30,
    weddingChargeMode: "percentage",
    weddingChargeValue: 25,
    updatedByEmail: AUTHORIZED_ADMIN_EMAIL,
    updatedAt: createdAt,
  };

  // Non-admins, other Gmail accounts, and unverified Admin CANNOT edit UPI settings, QR, Model Advance, or Wedding Charge
  await assertFails(
    aliceDb.collection("paymentConfig").doc("paymentSettings").set(disabledOfficialCfg)
  );
  await assertFails(
    otherGmailDb.collection("paymentConfig").doc("paymentSettings").set(disabledOfficialCfg)
  );
  await assertFails(
    unverifiedAdminDb.collection("paymentConfig").doc("paymentSettings").set(disabledOfficialCfg)
  );
  await assertSucceeds(
    adminDb.collection("paymentConfig").doc("paymentSettings").set(disabledOfficialCfg)
  );

  // 1b. Even Admin CANNOT activate payments without an uploaded QR or without qrVerified=true
  await assertFails(
    adminDb.collection("paymentConfig").doc("paymentSettings").set({
      ...disabledOfficialCfg,
      enabled: true,
      qrImageDataUrl: "",
      qrVerified: false,
    })
  );
  await assertFails(
    adminDb.collection("paymentConfig").doc("paymentSettings").set({
      ...disabledOfficialCfg,
      enabled: true,
      qrImageDataUrl: sampleUploadedQr,
      qrVerified: false,
    })
  );
  await assertFails(
    adminDb.collection("paymentConfig").doc("paymentSettings").set({
      ...disabledOfficialCfg,
      enabled: false,
      qrImageDataUrl: "",
      qrVerified: true,
    })
  );

  // 1c. Once Admin uploads QR and explicitly verifies it matches 9812890081@ybl -> SUCCEEDS
  const verifiedPaymentCfg = {
    ...disabledOfficialCfg,
    enabled: true,
    qrImageDataUrl: sampleUploadedQr,
    qrVerified: true,
  };
  await assertFails(
    otherGmailDb.collection("paymentConfig").doc("paymentSettings").set(verifiedPaymentCfg)
  );
  await assertSucceeds(
    adminDb.collection("paymentConfig").doc("paymentSettings").set(verifiedPaymentCfg)
  );

  // Signed-in customer can GET paymentConfig/paymentSettings (never LIST paymentConfig or read studioConfig)
  await assertSucceeds(aliceDb.collection("paymentConfig").doc("paymentSettings").get());
  await assertFails(aliceDb.collection("paymentConfig").get());
  await assertFails(aliceDb.collection("studioConfig").get());
  await assertFails(aliceDb.collection("studioConfig").doc("emailAlerts").get());
  await assertFails(unauthDb.collection("paymentConfig").doc("paymentSettings").get());

  // 2. Customer A submits a Model Advance Payment request (must start Payment Pending, paid=0)
  const modelPayBase = {
    customerUid: CUSTOMER_A_UID,
    customerName: "Aarav Sharma",
    customerContact: "+91 9811100001",
    bookingId: "",
    paymentCategory: "model_advance",
    modelId: "model_01",
    modelName: "Ananya Verma",
    serviceType: "Fashion Editorial Shoot",
    eventType: "Model Booking",
    shootDate: "2026-12-22",
    shootTime: "10:00 AM",
    eventLocation: "Star Artist Studio",
    eventDetails: "Editorial portfolio shoot",
    totalPrice: 0,
    chargeMode: "percentage",
    chargeValue: 0,
    requiredAdvance: 0,
    paidAmount: 0,
    remainingBalance: 0,
    priceSetByAdmin: false,
    paymentStatus: "Payment Pending",
    bookingStatus: "Payment Pending",
    utrNumber: "",
    screenshotDataUrl: "",
    verifiedAmount: 0,
    verifiedByAdminEmail: "",
    rejectionReason: "",
    refundAmount: 0,
    refundReason: "",
    refundStatus: "",
    refundReference: "",
    adminNotes: "",
    history: [],
    createdAt,
    updatedAt: createdAt,
  };

  await assertSucceeds(
    aliceDb.collection("bookingPayments").doc("pay_model_a1").set(modelPayBase)
  );

  // Customer A CANNOT forge Payment Verified or non-zero paidAmount on create
  await assertFails(
    aliceDb.collection("bookingPayments").doc("pay_forged_verified").set({
      ...modelPayBase,
      paymentStatus: "Payment Verified",
      bookingStatus: "Confirmed",
      paidAmount: 5000,
      verifiedAmount: 5000,
    })
  );

  // Customer B CANNOT read, list, or modify Customer A's payment record
  await assertFails(bobDb.collection("bookingPayments").doc("pay_model_a1").get());
  await assertFails(bobDb.collection("bookingPayments").get());
  await assertFails(unauthDb.collection("bookingPayments").doc("pay_model_a1").get());

  // 3. ONLY verified Admin can set Model Booking Advance / Wedding Booking Charge pricing
  await assertFails(
    otherGmailDb.collection("bookingPayments").doc("pay_model_a1").update({
      totalPrice: 10000,
      chargeMode: "percentage",
      chargeValue: 30,
      requiredAdvance: 3000,
      remainingBalance: 10000,
      priceSetByAdmin: true,
      updatedAt: pastDate(4000),
    })
  );
  await assertSucceeds(
    adminDb.collection("bookingPayments").doc("pay_model_a1").update({
      totalPrice: 10000,
      chargeMode: "percentage",
      chargeValue: 30,
      requiredAdvance: 3000,
      remainingBalance: 10000,
      priceSetByAdmin: true,
      updatedAt: pastDate(4000),
    })
  );

  // Customer A submits UTR atomically with utrClaims/{utrKey} (after Admin set pricing) -> status transitions ONLY to "Submitted for Verification"
  await assertSucceeds(
    (async () => {
      const batch = aliceDb.batch();
      const ts = pastDate(3000);
      batch.set(aliceDb.collection("utrClaims").doc("432198765432"), {
        utrKey: "432198765432",
        utrDisplay: "432198765432",
        paymentId: "pay_model_a1",
        claimedByUid: CUSTOMER_A_UID,
        status: "active",
        createdAt: ts,
      });
      batch.update(aliceDb.collection("bookingPayments").doc("pay_model_a1"), {
        utrNumber: "432198765432",
        utrKey: "432198765432",
        screenshotDataUrl: "",
        paymentStatus: "Submitted for Verification",
        submittedAt: ts,
        updatedAt: ts,
      });
      return batch.commit();
    })()
  );

  // Customer A CANNOT self-verify, self-confirm, tamper with history, or alter totalPrice/paidAmount/remainingBalance
  await assertFails(
    aliceDb.collection("bookingPayments").doc("pay_model_a1").update({
      paymentStatus: "Payment Verified",
      bookingStatus: "Confirmed",
      paidAmount: 3000,
      remainingBalance: 7000,
      updatedAt: pastDate(2000),
    })
  );
  await assertFails(
    aliceDb.collection("bookingPayments").doc("pay_model_a1").update({
      totalPrice: 100,
      updatedAt: pastDate(2000),
    })
  );
  await assertFails(
    aliceDb.collection("bookingPayments").doc("pay_model_a1").update({
      utrNumber: "432198765433",
      screenshotDataUrl: "",
      paymentStatus: "Submitted for Verification",
      submittedAt: pastDate(2000),
      history: [{ action: "Forged Admin Verification", status: "Fully Paid" }],
      updatedAt: pastDate(2000),
    })
  );

  // Another Gmail or unverified Admin CANNOT verify or reject customer payments
  await assertFails(
    otherGmailDb.collection("bookingPayments").doc("pay_model_a1").update({
      paymentStatus: "Partially Paid",
      bookingStatus: "Confirmed",
      paidAmount: 3000,
      verifiedAmount: 3000,
      remainingBalance: 7000,
      updatedAt: pastDate(1500),
    })
  );
  await assertFails(
    otherGmailDb.collection("bookingPayments").doc("pay_model_a1").update({
      paymentStatus: "Payment Rejected",
      rejectionReason: "Fake rejection",
      updatedAt: pastDate(1500),
    })
  );
  await assertFails(
    unverifiedAdminDb.collection("bookingPayments").doc("pay_model_a1").update({
      paymentStatus: "Partially Paid",
      bookingStatus: "Confirmed",
      paidAmount: 3000,
      verifiedAmount: 3000,
      remainingBalance: 7000,
      updatedAt: pastDate(1500),
    })
  );

  // 4. Authorized Admin rejects invalid submission, then verifies valid manual UPI payment -> Partially Paid + Confirmed
  //    Internal bank check note MUST be stored in strictly Admin-only adminPrivate subcollection (NOT customer-readable root doc)
  await assertSucceeds(
    adminDb.collection("bookingPayments").doc("pay_model_a1").update({
      paymentStatus: "Payment Rejected",
      rejectionReason: "UTR not found in bank statement",
      updatedAt: pastDate(1400),
    })
  );
  await assertFails(
    adminDb.collection("bookingPayments").doc("pay_model_a1").update({
      adminNotes: "Leaked internal note on root doc",
      updatedAt: pastDate(1300),
    })
  );
  await assertSucceeds(
    adminDb.collection("bookingPayments").doc("pay_model_a1").update({
      paidAmount: 3000,
      verifiedAmount: 3000,
      remainingBalance: 7000,
      paymentStatus: "Partially Paid",
      bookingStatus: "Confirmed",
      verifiedAt: pastDate(1200),
      verifiedByAdminEmail: AUTHORIZED_ADMIN_EMAIL,
      rejectionReason: "",
      adminNotes: "",
      updatedAt: pastDate(1200),
    })
  );
  await assertSucceeds(
    adminDb
      .collection("bookingPayments")
      .doc("pay_model_a1")
      .collection("adminPrivate")
      .doc("notes")
      .set({
        note: "Credited in official 9812890081@ybl bank statement",
        updatedByEmail: AUTHORIZED_ADMIN_EMAIL,
        updatedAt: pastDate(1200),
      })
  );
  // Customer A & B CANNOT read or write adminPrivate bank notes
  await assertFails(
    aliceDb
      .collection("bookingPayments")
      .doc("pay_model_a1")
      .collection("adminPrivate")
      .doc("notes")
      .get()
  );
  await assertFails(
    bobDb
      .collection("bookingPayments")
      .doc("pay_model_a1")
      .collection("adminPrivate")
      .doc("notes")
      .get()
  );

  // Customer A can request a refund on verified payment (without touching history), and Admin processes it to Refund Processed
  await assertSucceeds(
    aliceDb.collection("bookingPayments").doc("pay_model_a1").update({
      paymentStatus: "Refund Requested",
      refundStatus: "Requested",
      refundReason: "Event postponed by family",
      refundRequestedAt: pastDate(900),
      updatedAt: pastDate(900),
    })
  );
  await assertSucceeds(
    adminDb.collection("bookingPayments").doc("pay_model_a1").update({
      paymentStatus: "Refund Processed",
      refundStatus: "Processed",
      refundAmount: 3000,
      refundReference: "REFUND987654",
      refundProcessedAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );

  // Customer A CANNOT store prohibited credential fields (e.g. upiPin, otp, cardNumber, mpin, accountNumber)
  await assertFails(
    aliceDb.collection("bookingPayments").doc("pay_bad_secret").set({
      ...modelPayBase,
      upiPin: "1234",
    })
  );
  await assertFails(
    aliceDb.collection("bookingPayments").doc("pay_bad_mpin").set({
      ...modelPayBase,
      mpin: "9988",
    })
  );
});

// ============================================================================
// 10. AUDIT HARDENING VERIFICATION (HIGH #1 + MEDIUM #2, #3, #4, #5)
// ============================================================================

test("14. Payment Audit Hardening (High #1 & Medium #2): blocks forged create pricing/refunds, pre-pricing UTR submission, invalid screenshot payloads, and history tampering", async () => {
  const createdAt = pastDate(5000);
  const aliceDb = getCustomerAContext().firestore();
  const adminDb = getAdminContext().firestore();

  const validBase = {
    customerUid: CUSTOMER_A_UID,
    customerName: "Aarav Sharma",
    customerContact: "+91 9811100001",
    bookingId: "",
    paymentCategory: "wedding_booking",
    modelId: "",
    modelName: "",
    serviceType: "Wedding Photography & Film",
    eventType: "Wedding",
    shootDate: "2026-12-28",
    shootTime: "10:00 AM",
    eventLocation: "Sirsa Palace",
    eventDetails: "Two-day wedding coverage",
    totalPrice: 0,
    chargeMode: "percentage",
    chargeValue: 0,
    requiredAdvance: 0,
    paidAmount: 0,
    remainingBalance: 0,
    priceSetByAdmin: false,
    paymentStatus: "Payment Pending",
    bookingStatus: "Pending Admin Pricing",
    utrNumber: "",
    screenshotDataUrl: "",
    verifiedAmount: 0,
    verifiedByAdminEmail: "",
    rejectionReason: "",
    refundAmount: 0,
    refundReason: "",
    refundStatus: "",
    refundReference: "",
    adminNotes: "",
    history: [],
    createdAt,
    updatedAt: createdAt,
  };

  // High #1: Customer CANNOT forge non-zero totalPrice, requiredAdvance, refundStatus='Processed', refundAmount, or verifiedAt on create
  await assertFails(
    aliceDb.collection("bookingPayments").doc("pay_forge_price").set({
      ...validBase,
      totalPrice: 1,
      requiredAdvance: 1,
      remainingBalance: 0,
    })
  );
  await assertFails(
    aliceDb.collection("bookingPayments").doc("pay_forge_refund").set({
      ...validBase,
      refundStatus: "Processed",
      refundAmount: 25000,
    })
  );
  await assertFails(
    aliceDb.collection("bookingPayments").doc("pay_forge_verifiedAt").set({
      ...validBase,
      verifiedAt: pastDate(1000),
    })
  );
  await assertFails(
    aliceDb.collection("bookingPayments").doc("pay_forge_receipt").set({
      ...validBase,
      receiptNumber: "REC-FAKE-001",
    })
  );

  // Valid create succeeds
  await assertSucceeds(
    aliceDb.collection("bookingPayments").doc("pay_wed_audit_1").set(validBase)
  );

  // Medium #2: Customer CANNOT submit UTR before Admin sets price (priceSetByAdmin == false)
  await assertFails(
    aliceDb.collection("bookingPayments").doc("pay_wed_audit_1").update({
      utrNumber: "998877665544",
      screenshotDataUrl: "",
      paymentStatus: "Submitted for Verification",
      submittedAt: pastDate(2000),
      updatedAt: pastDate(2000),
    })
  );

  // Admin sets package price & advance
  await assertSucceeds(
    adminDb.collection("bookingPayments").doc("pay_wed_audit_1").update({
      totalPrice: 80000,
      chargeMode: "percentage",
      chargeValue: 25,
      requiredAdvance: 20000,
      remainingBalance: 80000,
      priceSetByAdmin: true,
      bookingStatus: "Payment Pending",
      updatedAt: pastDate(1800),
    })
  );

  // Medium #2: Customer CANNOT submit non-image screenshotDataUrl (XSS / invalid MIME)
  await assertFails(
    aliceDb.collection("bookingPayments").doc("pay_wed_audit_1").update({
      utrNumber: "998877665544",
      screenshotDataUrl: "javascript:alert(1)",
      paymentStatus: "Submitted for Verification",
      submittedAt: pastDate(1500),
      updatedAt: pastDate(1500),
    })
  );

  // Valid compressed image data URL succeeds atomically with utrClaims/{utrKey}
  const validShot = "data:image/jpeg;base64," + "B".repeat(160);
  await assertSucceeds(
    (async () => {
      const batch = aliceDb.batch();
      const ts = pastDate(1400);
      batch.set(aliceDb.collection("utrClaims").doc("998877665544"), {
        utrKey: "998877665544",
        utrDisplay: "998877665544",
        paymentId: "pay_wed_audit_1",
        claimedByUid: CUSTOMER_A_UID,
        status: "active",
        createdAt: ts,
      });
      batch.update(aliceDb.collection("bookingPayments").doc("pay_wed_audit_1"), {
        utrNumber: "998877665544",
        utrKey: "998877665544",
        screenshotDataUrl: validShot,
        paymentStatus: "Submitted for Verification",
        submittedAt: ts,
        updatedAt: ts,
      });
      return batch.commit();
    })()
  );
});

test("15. Audit Hardening (Medium #3, #4 & #5): Admin-only internal notes, professional read lock until Confirmed/Assigned, and Confirmed booking immutability", async () => {
  const createdAt = pastDate(5000);
  const aliceDb = getCustomerAContext().firestore();
  const proDb = getCustomerBContext().firestore(); // Acts as assigned professional
  const adminDb = getAdminContext().firestore();

  // Medium #3: Strictly Admin-only adminInternalNotes collection
  await assertSucceeds(
    adminDb.collection("adminInternalNotes").doc("note_bk_1").set({
      targetCollection: "bookings",
      targetId: "bk_conf_1",
      note: "Private studio margin and staff assignment note",
      updatedByEmail: AUTHORIZED_ADMIN_EMAIL,
      createdAt,
      updatedAt: createdAt,
    })
  );
  await assertFails(aliceDb.collection("adminInternalNotes").doc("note_bk_1").get());
  await assertFails(aliceDb.collection("adminInternalNotes").get());
  await assertFails(proDb.collection("adminInternalNotes").doc("note_bk_1").get());

  // Medium #4: Professional CANNOT read hiring request while status is Assignment Proposed / Proposal Declined / Cancelled
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("professionalHiringRequests").doc("hire_proposed_1").set({
      userId: CUSTOMER_A_UID,
      customerName: "Aarav Sharma",
      contactNumber: "+91 9811100001",
      eventType: "Wedding",
      requiredService: "Both",
      eventDate: "2026-12-26",
      eventTime: "07:00 PM",
      eventLocation: "Sirsa Royal Lawn",
      workingDuration: "2 days",
      budgetRange: "₹60,000",
      status: "Assignment Proposed",
      assignedProfessionalId: "pro_b_1",
      assignedProfessionalUid: CUSTOMER_B_UID,
      assignedProfessionalName: "Meera Photo",
      customerAcceptance: "pending",
      createdAt,
      updatedAt: createdAt,
    });
    await db.collection("outsourcingRequests").doc("out_pending_1").set({
      userId: CUSTOMER_A_UID,
      studioOrName: "Aarav Studio",
      contactInfo: "+91 9811100001",
      city: "Sirsa",
      workType: "Videography",
      projectDescription: "Need two cinematographers for evening wedding coverage.",
      workDeadline: "2026-12-29",
      status: "Pending Admin Review",
      assignedProfessionalId: "pro_b_1",
      assignedProfessionalUid: CUSTOMER_B_UID,
      assignedProfessionalName: "Meera Photo",
      createdAt,
      updatedAt: createdAt,
    });
    await db.collection("bookings").doc("bk_confirmed_a1").set({
      userId: CUSTOMER_A_UID,
      clientName: "Aarav Sharma",
      contactNumber: "+91 9811100001",
      serviceType: "Wedding Photography",
      preferredDate: "2026-12-30",
      preferredTime: "10:00 AM",
      location: "Sirsa",
      status: "Confirmed",
      createdAt,
      updatedAt: createdAt,
    });
    await db.collection("companyBookings").doc("comp_approved_a1").set({
      userId: CUSTOMER_A_UID,
      companyName: "Aarav Events",
      organizationType: "Company",
      contactPerson: "Aarav Sharma",
      mobileNumber: "+91 9811100001",
      email: "aarav@events.in",
      address: "Sector 10, Sirsa",
      modelRequirement: "Female Models",
      modelsCount: 2,
      eventType: "Fashion Show",
      eventDate: "2026-12-31",
      eventLocation: "Sirsa Hall",
      budget: "₹50,000",
      detailedRequirements: "Two runway models required for winter fashion evening.",
      status: "Approved",
      createdAt,
      updatedAt: createdAt,
    });
  });

  // Customer A can still read their own proposed hiring request, but assigned pro is BLOCKED until Confirmed/Assigned
  await assertSucceeds(
    aliceDb.collection("professionalHiringRequests").doc("hire_proposed_1").get()
  );
  await assertFails(
    proDb.collection("professionalHiringRequests").doc("hire_proposed_1").get()
  );
  await assertFails(
    proDb.collection("outsourcingRequests").doc("out_pending_1").get()
  );

  // Once Admin confirms/assigns the request, the assigned professional CAN read their active assignment
  await assertSucceeds(
    adminDb.collection("professionalHiringRequests").doc("hire_proposed_1").update({
      status: "Confirmed",
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    proDb.collection("professionalHiringRequests").doc("hire_proposed_1").get()
  );
  await assertSucceeds(
    adminDb.collection("outsourcingRequests").doc("out_pending_1").update({
      status: "Assigned",
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    proDb.collection("outsourcingRequests").doc("out_pending_1").get()
  );

  // Medium #5: Customer A CANNOT edit, cancel, or delete an already Confirmed booking or Approved companyBooking
  await assertFails(
    aliceDb.collection("bookings").doc("bk_confirmed_a1").update({
      preferredDate: "2027-01-15",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    aliceDb.collection("bookings").doc("bk_confirmed_a1").update({
      status: "Cancelled",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(aliceDb.collection("bookings").doc("bk_confirmed_a1").delete());
  await assertFails(
    aliceDb.collection("companyBookings").doc("comp_approved_a1").update({
      status: "Cancelled",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    aliceDb.collection("companyBookings").doc("comp_approved_a1").delete()
  );

  // Admin CAN still manage and delete Confirmed bookings
  await assertSucceeds(
    adminDb.collection("bookings").doc("bk_confirmed_a1").update({
      status: "Completed",
      updatedAt: pastDate(500),
    })
  );
});

test("16. Atomic Duplicate UTR Protection: simultaneous submissions by two customers accept ONLY ONE, block direct bypass, hide utrClaims from customers, and preserve legacy records", async () => {
  const createdAt = pastDate(6000);
  const aliceDb = getCustomerAContext().firestore();
  const bobDb = getCustomerBContext().firestore();
  const adminDb = getAdminContext().firestore();

  const makePricedPayment = (uid, name) => ({
    customerUid: uid,
    customerName: name,
    customerContact: "+91 9811100009",
    bookingId: "",
    paymentCategory: "wedding_booking",
    modelId: "",
    modelName: "",
    serviceType: "Wedding Photography",
    eventType: "Wedding",
    shootDate: "2026-12-27",
    shootTime: "10:00 AM",
    eventLocation: "Sirsa",
    eventDetails: "Full wedding shoot",
    totalPrice: 50000,
    chargeMode: "percentage",
    chargeValue: 25,
    requiredAdvance: 12500,
    paidAmount: 0,
    remainingBalance: 50000,
    priceSetByAdmin: true,
    paymentStatus: "Payment Pending",
    bookingStatus: "Payment Pending",
    utrNumber: "",
    utrKey: "",
    screenshotDataUrl: "",
    verifiedAmount: 0,
    verifiedByAdminEmail: "",
    rejectionReason: "",
    refundAmount: 0,
    refundReason: "",
    refundStatus: "",
    refundReference: "",
    adminNotes: "",
    history: [],
    createdAt,
    updatedAt: createdAt,
  });

  // Seed two priced customer payment records + one legacy payment record (without utrKey)
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("bookingPayments").doc("pay_race_alice").set(makePricedPayment(CUSTOMER_A_UID, "Alice"));
    await db.collection("bookingPayments").doc("pay_race_bob").set(makePricedPayment(CUSTOMER_B_UID, "Bob"));
    await db.collection("bookingPayments").doc("pay_alice_second").set(makePricedPayment(CUSTOMER_A_UID, "Alice"));
    const legacyDoc = makePricedPayment(CUSTOMER_A_UID, "Alice Legacy");
    delete legacyDoc.utrKey;
    legacyDoc.utrNumber = "LEGACY998877";
    legacyDoc.paymentStatus = "Submitted for Verification";
    await db.collection("bookingPayments").doc("pay_legacy_keep").set(legacyDoc);
  });

  // 1. Direct client bypass without atomic utrClaims/{utrKey} MUST FAIL
  await assertFails(
    aliceDb.collection("bookingPayments").doc("pay_race_alice").update({
      utrNumber: "UTR555666777888",
      utrKey: "UTR555666777888",
      screenshotDataUrl: "",
      paymentStatus: "Submitted for Verification",
      submittedAt: pastDate(2000),
      updatedAt: pastDate(2000),
    })
  );

  // 2. Simultaneous race condition: Customer A and Customer B submit the SAME normalized UTR at the exact same time
  const submitAtomicUtr = (db, uid, paymentId, displayUtr, utrKey) =>
    db.runTransaction(async (tx) => {
      const payRef = db.collection("bookingPayments").doc(paymentId);
      const claimRef = db.collection("utrClaims").doc(utrKey);
      const snap = await tx.get(payRef);
      if (!snap.exists) throw new Error("missing");
      const ts = pastDate(1500);
      tx.set(claimRef, {
        utrKey,
        utrDisplay: displayUtr,
        paymentId,
        claimedByUid: uid,
        status: "active",
        createdAt: ts,
      });
      tx.update(payRef, {
        utrNumber: displayUtr,
        utrKey,
        screenshotDataUrl: "",
        paymentStatus: "Submitted for Verification",
        submittedAt: ts,
        updatedAt: ts,
      });
    });

  const SHARED_UTR_KEY = "UTR778899001122";
  const raceResults = await Promise.allSettled([
    submitAtomicUtr(aliceDb, CUSTOMER_A_UID, "pay_race_alice", "UTR778899001122", SHARED_UTR_KEY),
    submitAtomicUtr(bobDb, CUSTOMER_B_UID, "pay_race_bob", "UTR-7788-9900-1122", SHARED_UTR_KEY),
  ]);

  const succeededCount = raceResults.filter((r) => r.status === "fulfilled").length;
  const rejectedCount = raceResults.filter((r) => r.status === "rejected").length;
  if (succeededCount !== 1 || rejectedCount !== 1) {
    throw new Error(
      `Expected EXACTLY 1 winner and 1 rejection in simultaneous duplicate UTR race, got ${succeededCount} fulfilled / ${rejectedCount} rejected`
    );
  }

  // 3. Subsequent duplicate attempt by same customer on another booking MUST FAIL
  await assertFails(
    submitAtomicUtr(aliceDb, CUSTOMER_A_UID, "pay_alice_second", "UTR778899001122", SHARED_UTR_KEY)
  );

  // 4. Privacy: Customers CANNOT read, list, update, or delete utrClaims; Admin CAN read utrClaims
  await assertFails(aliceDb.collection("utrClaims").doc(SHARED_UTR_KEY).get());
  await assertFails(bobDb.collection("utrClaims").doc(SHARED_UTR_KEY).get());
  await assertFails(aliceDb.collection("utrClaims").get());
  await assertFails(aliceDb.collection("utrClaims").doc(SHARED_UTR_KEY).delete());
  await assertSucceeds(adminDb.collection("utrClaims").doc(SHARED_UTR_KEY).get());
  await assertSucceeds(adminDb.collection("utrClaims").get());

  // 5. Legacy record (without utrKey) remains 100% readable by owner and verifiable by Admin untouched
  await assertSucceeds(aliceDb.collection("bookingPayments").doc("pay_legacy_keep").get());
  await assertSucceeds(
    adminDb.collection("bookingPayments").doc("pay_legacy_keep").update({
      paidAmount: 12500,
      verifiedAmount: 12500,
      remainingBalance: 37500,
      paymentStatus: "Partially Paid",
      bookingStatus: "Confirmed",
      verifiedAt: pastDate(500),
      verifiedByAdminEmail: AUTHORIZED_ADMIN_EMAIL,
      rejectionReason: "",
      adminNotes: "",
      updatedAt: pastDate(500),
    })
  );
});


