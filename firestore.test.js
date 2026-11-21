const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
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

// --- SECURITY BOUNDS TESTS ---

test("Unauthenticated user: cannot read models or bookings", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("models").get());
  await assertFails(unauthDb.collection("bookings").get());
});

test("Public user: can read approved gallery items", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("gallery").doc("item_1").set({
      userId: ADMIN_UID,
      title: "Fashion Shoot Demo",
      category: "Fashion",
      mediaType: "photo",
      mediaUrl: "https://example.com/demo.jpg",
      isDemo: true,
      status: "approved",
      createdAt: new Date(),
    });
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(unauthDb.collection("gallery").doc("item_1").get());
});

test("Authenticated user: cannot read another user's model registration", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("models").doc("bob_model").set({
      userId: BOB_UID,
      fullName: "Bob Model",
      age: 22,
      city: "Sirsa",
      phoneNumber: "9812890081",
      consentGiven: true,
      status: "pending",
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("models").doc("bob_model").get());
});

test("Authenticated user: can create valid model registration with 18+ age", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("models").doc("alice_model").set({
      userId: ALICE_UID,
      fullName: "Alice Wonder",
      age: 21,
      city: "Sirsa",
      phoneNumber: "9812890081",
      consentGiven: true,
      status: "pending",
      createdAt: new Date(),
    })
  );
});

test("Model registration: fails under 18 years old", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("models").doc("underage_model").set({
      userId: ALICE_UID,
      fullName: "Underage Model",
      age: 17,
      city: "Sirsa",
      phoneNumber: "9812890081",
      consentGiven: true,
      status: "pending",
      createdAt: new Date(),
    })
  );
});

test("Booking creation: authenticated client can create Pending bookings for Modeling, Photography, and Video Production", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const services = ["Modeling", "Photography", "Video Production"];

  for (let i = 0; i < services.length; i++) {
    await assertSucceeds(
      aliceDb.collection("bookings").doc(`alice_booking_${i}`).set({
        userId: ALICE_UID,
        clientName: "Alice Client",
        contactNumber: "9812890081",
        serviceType: services[i],
        preferredDate: "2026-11-20",
        preferredTime: "10:00 AM (Morning Key Light Bay)",
        location: "Studio (Talwara Khurd - 4K Lighting Bay)",
        status: "Pending",
        createdAt: new Date(),
      })
    );
  }

  // Cross-user read is denied
  await assertFails(bobDb.collection("bookings").doc("alice_booking_0").get());

  // Non-admin client cannot forge an already-Confirmed booking on create
  await assertFails(
    aliceDb.collection("bookings").doc("alice_forged_status").set({
      userId: ALICE_UID,
      clientName: "Alice Client",
      contactNumber: "9812890081",
      serviceType: "Video Production",
      preferredDate: "2026-11-21",
      status: "Confirmed",
      createdAt: new Date(),
    })
  );

  // Empty serviceType or preferredDate is rejected
  await assertFails(
    aliceDb.collection("bookings").doc("alice_empty_service").set({
      userId: ALICE_UID,
      clientName: "Alice Client",
      contactNumber: "9812890081",
      serviceType: "",
      preferredDate: "2026-11-21",
      status: "Pending",
      createdAt: new Date(),
    })
  );
});

test("Admin approval: designated admin can approve model registration", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("models").doc("model_pending").set({
      userId: ALICE_UID,
      fullName: "Alice Wonder",
      age: 21,
      city: "Sirsa",
      phoneNumber: "9812890081",
      consentGiven: true,
      status: "pending",
      createdAt: new Date(),
    });
  });

  const adminDb = testEnv.authenticatedContext("admin_uid", { email: "chanderstudio0@gmail.com", email_verified: true }).firestore();
  await assertSucceeds(
    adminDb.collection("models").doc("model_pending").update({
      status: "approved",
      adminNotes: "Approved by studio director",
      updatedAt: new Date(),
    })
  );
});

test("Admin approval: designated admin can update booking status to Confirmed", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("bookings").doc("booking_pending").set({
      userId: ALICE_UID,
      clientName: "Alice Client",
      contactNumber: "9812890081",
      serviceType: "Photography",
      preferredDate: "2026-11-20",
      status: "Pending",
      createdAt: new Date(),
    });
  });

  const adminDb = testEnv.authenticatedContext("admin_uid", { email: "chanderstudio0@gmail.com", email_verified: true }).firestore();
  await assertSucceeds(
    adminDb.collection("bookings").doc("booking_pending").update({
      status: "Confirmed",
      updatedAt: new Date(),
    })
  );
});

test("Gallery review: admin can approve user-uploaded gallery item", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("gallery").doc("user_gallery_pending").set({
      userId: ALICE_UID,
      title: "Royal Portrait",
      category: "Fashion",
      mediaType: "photo",
      mediaUrl: "https://example.com/portrait.jpg",
      isDemo: false,
      status: "pending",
      createdAt: new Date(),
    });
  });

  const adminDb = testEnv.authenticatedContext("admin_uid", { email: "chanderstudio0@gmail.com", email_verified: true }).firestore();
  await assertSucceeds(
    adminDb.collection("gallery").doc("user_gallery_pending").update({
      status: "approved",
      updatedAt: new Date(),
    })
  );
});

test("User history: authenticated user can record generation history in user subcollection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("history").doc("history_1").set({
      userId: ALICE_UID,
      category: "video",
      title: "Cinematic Music Video",
      parametersSummary: "High-Energy • 30s",
      content: "Storyboard Content",
      isOfflineFallback: true,
      timestamp: new Date(),
    })
  );
});

// --- BLOG POSTS SECURITY TESTS ---

test("Blog: public and regular users can read published blog posts", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("posts").doc("post_1").set({
      authorId: "admin_uid",
      authorName: "Chander Studio",
      title: "5 Lighting Setups for Cinematic Portraits",
      category: "Photography Tips",
      content: "Discover three-point lighting techniques used at Star Artist Studio in Talwara Khurd...",
      summary: "Master cinematic studio lighting with key, fill, and rim lights.",
      imageUrl: "https://example.com/lighting.jpg",
      tags: "lighting,portrait,cinematic",
      status: "published",
      readTimeMinutes: 4,
      createdAt: new Date(),
    });
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(unauthDb.collection("posts").doc("post_1").get());

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("posts").doc("post_1").get());
});

test("Blog: non-admin user cannot create blog posts", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("posts").doc("post_unauthorized").set({
      authorId: ALICE_UID,
      title: "Unauthorized Post",
      category: "Studio News",
      content: "This should fail because Alice is not a studio administrator.",
      status: "published",
      createdAt: new Date(),
    })
  );
});

test("Blog: authorized admin (chanderstudio0@gmail.com) can create and delete blog posts", async () => {
  const adminDb = testEnv.authenticatedContext("admin_uid", { email: "chanderstudio0@gmail.com", email_verified: true }).firestore();
  await assertSucceeds(
    adminDb.collection("posts").doc("admin_post_1").set({
      authorId: "admin_uid",
      authorName: "Star Artist Studio",
      title: "Behind The Scenes: New 4K Production Stage",
      category: "Behind The Scenes",
      content: "We have upgraded our studio facilities in Talwara Khurd, Sirsa with high-CRI continuous lighting grids.",
      summary: "Tour our upgraded 4K cinema stage in Talwara Khurd, Sirsa.",
      tags: "bts,studio,gear",
      status: "published",
      readTimeMinutes: 3,
      createdAt: new Date(),
    })
  );

  // Admin can update
  await assertSucceeds(
    adminDb.collection("posts").doc("admin_post_1").update({
      title: "Behind The Scenes: New 4K Production Stage (Updated)",
      updatedAt: new Date(),
    })
  );

  // Admin can delete
  await assertSucceeds(
    adminDb.collection("posts").doc("admin_post_1").delete()
  );
});

// --- ARTIST USER PROFILE SECURITY TESTS ---

test("User Profile: authenticated owner can update contact info, displayName, and avatarUrl", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const createdAt = new Date(Date.now() - 10000);

  await aliceDb.collection("users").doc(ALICE_UID).set({
    userId: ALICE_UID,
    email: "alice@starartist.com",
    displayName: "Alice",
    role: "user",
    createdAt,
    updatedAt: createdAt,
  });

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Star Artist",
      avatarUrl: "https://example.com/alice_avatar.jpg",
      photoUrl: "https://example.com/alice_avatar.jpg",
      phoneNumber: "+91 9812890081",
      city: "Sirsa, Haryana",
      instagramHandle: "@alice_model",
      portfolioLinks: [
        "https://instagram.com/alice_model",
        "https://behance.net/alicestudio",
      ],
      specialty: "Fashion Model",
      bio: "Editorial and runway model based in Haryana.",
      updatedAt: new Date(),
    })
  );

  // Rejects > 10 portfolio links
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      portfolioLinks: Array.from({ length: 11 }, (_, i) => `https://example.com/p/${i}`),
      updatedAt: new Date(),
    })
  );
});

test("User Profile: user cannot escalate role or update another user's profile", async () => {
  const createdAt = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@starartist.com",
      displayName: "Alice",
      role: "user",
      createdAt,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "admin",
      updatedAt: new Date(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hacked Name",
      updatedAt: new Date(),
    })
  );
});

test("User Profile: owner can save favoritePackageIds up to 30 items and rejects invalid or cross-user writes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const createdAt = new Date(Date.now() - 10000);

  await aliceDb.collection("users").doc(ALICE_UID).set({
    userId: ALICE_UID,
    email: "alice@starartist.com",
    displayName: "Alice",
    role: "user",
    favoritePackageIds: ["photo_editorial"],
    createdAt,
    updatedAt: createdAt,
  });

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      favoritePackageIds: ["photo_editorial", "modeling_portfolio", "prod_music_video"],
      updatedAt: new Date(),
    })
  );

  // Rejects > 30 favorite packages
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      favoritePackageIds: Array.from({ length: 31 }, (_, i) => `pkg_${i}`),
      updatedAt: new Date(),
    })
  );

  // Rejects non-list favoritePackageIds
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      favoritePackageIds: "photo_editorial",
      updatedAt: new Date(),
    })
  );

  // Cross-user update denied
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      favoritePackageIds: ["modeling_portfolio"],
      updatedAt: new Date(),
    })
  );
});

// --- SERVICE REVIEWS SECURITY TESTS ---

test("Reviews: unauthenticated user cannot read or write reviews", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("reviews").get());
  await assertFails(
    unauthDb.collection("reviews").doc("rev_unauth").set({
      userId: ALICE_UID,
      bookingId: "booking_1",
      reviewerName: "Alice",
      serviceType: "Photography",
      rating: 5,
      comment: "Great studio experience!",
      createdAt: new Date(),
    })
  );
});

test("Reviews: authenticated user can create, read, and update their own review", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const createdAt = new Date(Date.now() - 5000);
  const reviewId = `${ALICE_UID}_booking_101`;

  await assertSucceeds(
    aliceDb.collection("reviews").doc(reviewId).set({
      userId: ALICE_UID,
      bookingId: "booking_101",
      reviewerName: "Alice Artist",
      reviewerAvatarUrl: "https://example.com/alice.jpg",
      serviceType: "Photography (High-Fashion Editorial)",
      rating: 5,
      comment: "Amazing studio lighting bay and super helpful photography director!",
      createdAt,
      updatedAt: createdAt,
    })
  );

  await assertSucceeds(aliceDb.collection("reviews").doc(reviewId).get());

  await assertSucceeds(
    aliceDb.collection("reviews").doc(reviewId).update({
      rating: 4,
      comment: "Updated review: great lighting and smooth post-production.",
      updatedAt: new Date(),
    })
  );
});

test("Reviews: rejects invalid ratings, short comments, and cross-user tampering", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const createdAt = new Date(Date.now() - 5000);

  await assertFails(
    aliceDb.collection("reviews").doc("bad_rating_zero").set({
      userId: ALICE_UID,
      bookingId: "booking_102",
      reviewerName: "Alice Artist",
      serviceType: "Photography",
      rating: 0,
      comment: "Rating zero should fail rules.",
      createdAt,
    })
  );

  await assertFails(
    aliceDb.collection("reviews").doc("bad_rating_six").set({
      userId: ALICE_UID,
      bookingId: "booking_103",
      reviewerName: "Alice Artist",
      serviceType: "Photography",
      rating: 6,
      comment: "Rating six should fail rules.",
      createdAt,
    })
  );

  await assertFails(
    aliceDb.collection("reviews").doc("short_comment").set({
      userId: ALICE_UID,
      bookingId: "booking_104",
      reviewerName: "Alice Artist",
      serviceType: "Photography",
      rating: 5,
      comment: "Ok",
      createdAt,
    })
  );

  const validReviewId = `${ALICE_UID}_booking_105`;
  await aliceDb.collection("reviews").doc(validReviewId).set({
    userId: ALICE_UID,
    bookingId: "booking_105",
    reviewerName: "Alice Artist",
    serviceType: "Modeling",
    rating: 5,
    comment: "Wonderful experience with the studio casting team!",
    createdAt,
    updatedAt: createdAt,
  });

  await assertFails(
    bobDb.collection("reviews").doc(validReviewId).update({
      rating: 1,
      comment: "Tampered by another user",
      updatedAt: new Date(),
    })
  );

  await assertFails(bobDb.collection("reviews").doc(validReviewId).delete());
});

// --- ADMIN PANEL & FCM SYNC PROTECTION TESTS ---

test("Admin protection: unverified admin email token is DENIED admin privileges", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("bookings").doc("booking_spoof_check").set({
      userId: ALICE_UID,
      clientName: "Alice Client",
      contactNumber: "9812890081",
      serviceType: "Photography",
      preferredDate: "2026-12-01",
      status: "Pending",
      createdAt: new Date(),
    });
  });

  const unverifiedAdminDb = testEnv
    .authenticatedContext("spoof_admin_uid", { email: "chanderstudio0@gmail.com", email_verified: false })
    .firestore();

  await assertFails(
    unverifiedAdminDb.collection("bookings").doc("booking_spoof_check").update({
      status: "Confirmed",
      updatedAt: new Date(),
    })
  );

  await assertFails(
    unverifiedAdminDb.collection("posts").doc("spoof_post").set({
      authorId: "spoof_admin_uid",
      title: "Spoof Post",
      category: "Studio News",
      content: "Should fail because email_verified is false.",
      status: "published",
      createdAt: new Date(),
    })
  );
});

test("Admin protection: regular users cannot list admins collection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("admins").get());
  await assertFails(aliceDb.collection("models").get());
  await assertFails(aliceDb.collection("bookings").get());
});

test("User Profile: authenticated owner can sync fcmToken and fcmTokenUpdatedAt", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const createdAt = new Date(Date.now() - 10000);

  await aliceDb.collection("users").doc(ALICE_UID).set({
    userId: ALICE_UID,
    email: "alice@starartist.com",
    displayName: "Alice",
    role: "user",
    createdAt,
    updatedAt: createdAt,
  });

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      fcmToken: "sample_fcm_token_xyz_123456",
      fcmTokenUpdatedAt: new Date(),
    })
  );
});

test("Gallery upload: allows high-quality base64 portfolio photo up to 850KB and rejects oversized payload", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const validDataUri = "data:image/jpeg;base64," + "A".repeat(120000);
  const oversizedDataUri = "data:image/jpeg;base64," + "A".repeat(860000);

  await assertSucceeds(
    aliceDb.collection("gallery").doc("alice_hd_photo").set({
      userId: ALICE_UID,
      title: "Studio Editorial Shot",
      category: "Fashion",
      mediaType: "photo",
      mediaUrl: validDataUri,
      thumbnailUrl: "",
      isDemo: false,
      status: "pending",
      createdAt: new Date(),
    })
  );

  await assertFails(
    aliceDb.collection("gallery").doc("alice_oversized_photo").set({
      userId: ALICE_UID,
      title: "Too Large Photo",
      category: "Fashion",
      mediaType: "photo",
      mediaUrl: oversizedDataUri,
      thumbnailUrl: "",
      isDemo: false,
      status: "pending",
      createdAt: new Date(),
    })
  );
});

// --- STUDIO ANNOUNCEMENTS & APPOINTMENT NOTIFICATIONS (FCM) ---

test("FCM Preferences: user can update announcementsOptIn and appointmentPushOptIn on own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const createdAt = new Date(Date.now() - 10000);

  await aliceDb.collection("users").doc(ALICE_UID).set({
    userId: ALICE_UID,
    email: "alice@starartist.com",
    displayName: "Alice",
    role: "user",
    announcementsOptIn: true,
    appointmentPushOptIn: true,
    createdAt,
    updatedAt: createdAt,
  });

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      announcementsOptIn: false,
      appointmentPushOptIn: true,
      fcmToken: "fcm_token_updated_abc",
      fcmTokenUpdatedAt: new Date(),
      updatedAt: new Date(),
    })
  );

  // Rejects non-boolean type
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      announcementsOptIn: "yes",
      updatedAt: new Date(),
    })
  );
});

test("Announcements: admin can publish & delete studio announcements; signed-in users can read; non-admins cannot write", async () => {
  const adminDb = testEnv.authenticatedContext("admin_uid", { email: "chanderstudio0@gmail.com", email_verified: true }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  await assertSucceeds(
    adminDb.collection("announcements").doc("announce_01").set({
      authorId: "admin_uid",
      authorName: "Chander Studio",
      title: "Monsoon Editorial Casting Call Open!",
      message: "Star Artist Studio is hosting an editorial casting call this Sunday at our 4K soundstage.",
      category: "Casting Call",
      priority: "urgent",
      targetTopic: "studio_announcements",
      actionRoute: "models",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  // Signed-in user can read announcements
  await assertSucceeds(aliceDb.collection("announcements").get());
  await assertSucceeds(aliceDb.collection("announcements").doc("announce_01").get());

  // Unauthenticated user cannot read announcements
  await assertFails(unauthDb.collection("announcements").get());

  // Non-admin user cannot create, update, or delete announcements
  await assertFails(
    aliceDb.collection("announcements").doc("announce_forged").set({
      authorId: ALICE_UID,
      title: "Unauthorized Broadcast",
      message: "Non-admin should not broadcast announcements.",
      category: "Studio Update",
      priority: "normal",
      createdAt: new Date(),
    })
  );
  await assertFails(aliceDb.collection("announcements").doc("announce_01").delete());

  // Admin can delete
  await assertSucceeds(adminDb.collection("announcements").doc("announce_01").delete());
});

test("Appointment Notifications: admin can notify user, owner can read & mark read, cross-user blocked", async () => {
  const adminDb = testEnv.authenticatedContext("admin_uid", { email: "chanderstudio0@gmail.com", email_verified: true }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const createdAt = new Date(Date.now() - 5000);

  // Admin creates notification in Alice's notifications subcollection
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_01").set({
      userId: ALICE_UID,
      bookingId: "booking_abc",
      title: "Shoot Booking Confirmed!",
      message: "Your Photography shoot is confirmed for 2026-12-01.",
      status: "Confirmed",
      serviceType: "Photography",
      targetFcmToken: "alice_fcm_token",
      isRead: false,
      channelType: "appointment_status",
      createdAt,
      updatedAt: createdAt,
    })
  );

  // Alice can list and read her notifications
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("notifications").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_01").get());

  // Alice can mark her notification as read
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_01").update({
      isRead: true,
      updatedAt: new Date(),
    })
  );

  // Alice cannot forge status or bookingId when updating her own notification
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_01").update({
      status: "Completed",
      updatedAt: new Date(),
    })
  );

  // Bob cannot read, create, or delete Alice's notifications
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("notifications").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_forged").set({
      userId: ALICE_UID,
      bookingId: "booking_xyz",
      title: "Forged Alert",
      message: "Cross-user write attempt.",
      status: "Confirmed",
      isRead: false,
      createdAt: new Date(),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_01").delete());

  // Invalid status is rejected even from admin
  await assertFails(
    adminDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_bad").set({
      userId: ALICE_UID,
      bookingId: "booking_abc",
      title: "Invalid Status",
      message: "Should fail rules.",
      status: "HackedStatus",
      isRead: false,
      createdAt: new Date(),
    })
  );
});

// --- STRICT SINGLE-GMAIL ADMIN ACCESS TESTS ---

test("Strict Admin: another verified Gmail account is DENIED all admin privileges even if listed in /admins/{uid}", async () => {
  const OTHER_GMAIL_UID = "other_gmail_uid";
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("admins").doc(OTHER_GMAIL_UID).set({
      email: "visitor@gmail.com",
      role: "admin",
    });
    await context.firestore().collection("bookings").doc("alice_booking_strict").set({
      userId: ALICE_UID,
      clientName: "Alice Client",
      contactNumber: "9812890081",
      serviceType: "Photography",
      preferredDate: "2026-12-15",
      status: "Pending",
      createdAt: new Date(),
    });
    await context.firestore().collection("models").doc("alice_model_strict").set({
      userId: ALICE_UID,
      fullName: "Alice Wonder",
      age: 22,
      city: "Sirsa",
      phoneNumber: "9812890081",
      consentGiven: true,
      status: "pending",
      createdAt: new Date(),
    });
    await context.firestore().collection("gallery").doc("alice_gallery_strict").set({
      userId: ALICE_UID,
      title: "Pending Shoot",
      category: "Fashion",
      mediaType: "photo",
      mediaUrl: "https://example.com/shoot.jpg",
      isDemo: false,
      status: "pending",
      createdAt: new Date(),
    });
  });

  const otherGmailDb = testEnv
    .authenticatedContext(OTHER_GMAIL_UID, { email: "visitor@gmail.com", email_verified: true })
    .firestore();

  // Cannot read own or others' /admins doc
  await assertFails(otherGmailDb.collection("admins").doc(OTHER_GMAIL_UID).get());
  await assertFails(otherGmailDb.collection("admins").get());

  // Cannot read or modify bookings, models, pending gallery, or announcements
  await assertFails(otherGmailDb.collection("bookings").doc("alice_booking_strict").get());
  await assertFails(
    otherGmailDb.collection("bookings").doc("alice_booking_strict").update({
      status: "Confirmed",
      updatedAt: new Date(),
    })
  );
  await assertFails(otherGmailDb.collection("models").doc("alice_model_strict").get());
  await assertFails(
    otherGmailDb.collection("models").doc("alice_model_strict").update({
      status: "approved",
      updatedAt: new Date(),
    })
  );
  await assertFails(
    otherGmailDb.collection("gallery").doc("alice_gallery_strict").update({
      status: "approved",
      updatedAt: new Date(),
    })
  );
  await assertFails(
    otherGmailDb.collection("announcements").doc("other_gmail_announce").set({
      authorId: OTHER_GMAIL_UID,
      title: "Unauthorized Broadcast",
      message: "Should be denied because email is not chanderstudio0@gmail.com.",
      category: "Studio Update",
      priority: "normal",
      createdAt: new Date(),
    })
  );
});

test("Booking integrity: regular owner can cancel own booking but CANNOT self-confirm or complete it", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("bookings").doc("alice_self_confirm").set({
      userId: ALICE_UID,
      clientName: "Alice Client",
      contactNumber: "9812890081",
      serviceType: "Modeling",
      preferredDate: "2026-12-20",
      status: "Pending",
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@gmail.com", email_verified: true }).firestore();

  await assertFails(
    aliceDb.collection("bookings").doc("alice_self_confirm").update({
      status: "Confirmed",
      updatedAt: new Date(),
    })
  );
  await assertFails(
    aliceDb.collection("bookings").doc("alice_self_confirm").update({
      status: "Completed",
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("bookings").doc("alice_self_confirm").update({
      status: "Cancelled",
      updatedAt: new Date(),
    })
  );
});

// --- COMPANY / EVENT ORGANIZER & PUBLIC MODEL ROSTER SECURITY TESTS ---

test("Public Models: public visitors can read approved roster; rules strictly block leaking phoneNumber/email", async () => {
  const adminDb = testEnv
    .authenticatedContext("admin_uid", { email: "chanderstudio0@gmail.com", email_verified: true })
    .firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  // Admin can publish privacy-safe public profile
  await assertSucceeds(
    adminDb.collection("publicModels").doc("model_pub_1").set({
      modelId: "model_pub_1",
      fullName: "Ananya Verma",
      gender: "Female",
      city: "Sirsa, Haryana",
      skills: "Runway, Bridal Editorial",
      portfolioUrl: "https://instagram.com/starartiststudio",
      avatarUrl: "/images/model-portrait.jpg",
      category: "Official Studio Roster",
      isOfficialRoster: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  // Public unauthenticated visitors can browse publicModels
  await assertSucceeds(unauthDb.collection("publicModels").doc("model_pub_1").get());
  await assertSucceeds(unauthDb.collection("publicModels").get());

  // Privacy guard: even Admin cannot accidentally store personal phoneNumber or email in publicModels
  await assertFails(
    adminDb.collection("publicModels").doc("model_pub_leak_phone").set({
      modelId: "model_pub_leak_phone",
      fullName: "Leaky Model",
      gender: "Female",
      city: "Sirsa",
      phoneNumber: "+91 9812890081",
      isOfficialRoster: true,
      createdAt: new Date(),
    })
  );
  await assertFails(
    adminDb.collection("publicModels").doc("model_pub_leak_email").set({
      modelId: "model_pub_leak_email",
      fullName: "Leaky Model",
      gender: "Female",
      city: "Sirsa",
      email: "private@example.com",
      isOfficialRoster: true,
      createdAt: new Date(),
    })
  );

  // Regular user cannot write or delete publicModels
  await assertFails(
    aliceDb.collection("publicModels").doc("model_pub_forged").set({
      modelId: "model_pub_forged",
      fullName: "Unauthorized Model",
      city: "Sirsa",
      isOfficialRoster: true,
      createdAt: new Date(),
    })
  );
  await assertFails(aliceDb.collection("publicModels").doc("model_pub_1").delete());
});

test("Companies: owner can register & update own company profile; cross-user denied; Admin can read", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv
    .authenticatedContext("admin_uid", { email: "chanderstudio0@gmail.com", email_verified: true })
    .firestore();
  const createdAt = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("companies").doc(ALICE_UID).set({
      userId: ALICE_UID,
      companyName: "Royal Heritage Events",
      organizationType: "Event Organizer",
      contactPerson: "Vikram Mehta",
      mobileNumber: "+91 9812890081",
      email: "events@royalheritage.in",
      address: "Sector 17, Chandigarh, India",
      createdAt,
      updatedAt: createdAt,
    })
  );

  await assertSucceeds(aliceDb.collection("companies").doc(ALICE_UID).get());
  await assertSucceeds(adminDb.collection("companies").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("companies").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("companies").doc(ALICE_UID).update({
      companyName: "Hijacked Co",
      updatedAt: new Date(),
    })
  );
});

test("Company Model Bookings: company can create Pending booking & cancel only; Admin can approve, contact, reject, and delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv
    .authenticatedContext("admin_uid", { email: "chanderstudio0@gmail.com", email_verified: true })
    .firestore();
  const createdAt = new Date(Date.now() - 5000);

  const validBooking = {
    userId: ALICE_UID,
    companyName: "Royal Heritage Events",
    organizationType: "Event Organizer",
    contactPerson: "Vikram Mehta",
    mobileNumber: "+91 9812890081",
    email: "events@royalheritage.in",
    address: "Sector 17, Chandigarh, India",
    modelRequirement: "Both",
    modelsCount: 6,
    eventType: "Fashion Show",
    eventDate: "2026-12-28",
    eventLocation: "Grand Ballroom, Sirsa",
    budget: "₹75,000",
    detailedRequirements: "Need 3 male and 3 female runway models for bridal & festive couture walk.",
    requestedModelIds: ["model_pub_1"],
    requestedModelNames: ["Ananya Verma"],
    status: "Pending",
    adminNotes: "",
    createdAt,
    updatedAt: createdAt,
  };

  await assertSucceeds(aliceDb.collection("companyBookings").doc("comp_req_1").set(validBooking));

  // Cross-user read denied
  await assertFails(bobDb.collection("companyBookings").doc("comp_req_1").get());

  // Company cannot forge Approved status on create or self-approve on update
  await assertFails(
    aliceDb.collection("companyBookings").doc("comp_req_forged").set({
      ...validBooking,
      status: "Approved",
    })
  );
  await assertFails(
    aliceDb.collection("companyBookings").doc("comp_req_1").update({
      status: "Approved",
      updatedAt: new Date(),
    })
  );

  // Admin can approve, mark Contacted, add adminNotes, and delete
  await assertSucceeds(
    adminDb.collection("companyBookings").doc("comp_req_1").update({
      status: "Approved",
      adminNotes: "Casting shortlist shared with organizer.",
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    adminDb.collection("companyBookings").doc("comp_req_1").update({
      status: "Contacted",
      updatedAt: new Date(),
    })
  );

  // Owner can cancel their own request
  await assertSucceeds(
    aliceDb.collection("companyBookings").doc("comp_req_1").update({
      status: "Cancelled",
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(adminDb.collection("companyBookings").doc("comp_req_1").delete());
});


