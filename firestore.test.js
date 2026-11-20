const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const SYNDIC_UID = "syndic_yassine";
const DEPUTY_UID = "deputy_nadia";
const RESIDENT_UID = "resident_karim";
const OUTSIDER_UID = "outsider_omar";
const BUILDING_ID = "bld_yasmina_1";
const OTHER_BUILDING_ID = "bld_atlas_2";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const pastTs = () => new Date(Date.now() - 5000);
const futureTs = () => new Date(Date.now() + 120000);

async function seedBuildingAndMembers() {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const ts = pastTs();
    await db.collection("buildings").doc(BUILDING_ID).set({
      name: "إقامة الياسمين",
      city: "الدار البيضاء",
      neighborhood: "المعاريف",
      floorCount: 5,
      apartmentCount: 10,
      defaultMonthlyFee: 250,
      reserveFundBalance: 12000,
      inviteCode: "YSM482",
      syndicUid: SYNDIC_UID,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("invites").doc("YSM482").set({
      buildingId: BUILDING_ID,
      buildingName: "إقامة الياسمين",
      city: "الدار البيضاء",
      syndicUid: SYNDIC_UID,
      active: true,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("buildings").doc(BUILDING_ID).collection("members").doc(SYNDIC_UID).set({
      uid: SYNDIC_UID,
      displayName: "ياسين السانديك",
      apartmentId: "apt_1",
      apartmentLabel: "طابق 1 - شقة 1",
      occupancyType: "OWNER",
      role: "SYNDIC",
      status: "APPROVED",
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("buildings").doc(BUILDING_ID).collection("members").doc(DEPUTY_UID).set({
      uid: DEPUTY_UID,
      displayName: "نادية النائبة",
      apartmentId: "apt_2",
      apartmentLabel: "طابق 1 - شقة 2",
      occupancyType: "OWNER",
      role: "DEPUTY",
      status: "APPROVED",
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("buildings").doc(BUILDING_ID).collection("members").doc(RESIDENT_UID).set({
      uid: RESIDENT_UID,
      displayName: "كريم الساكن",
      apartmentId: "apt_3",
      apartmentLabel: "طابق 2 - شقة 3",
      occupancyType: "TENANT",
      role: "RESIDENT",
      status: "APPROVED",
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("buildings").doc(OTHER_BUILDING_ID).set({
      name: "عمارة الأطلس",
      city: "الرباط",
      floorCount: 4,
      apartmentCount: 8,
      defaultMonthlyFee: 300,
      reserveFundBalance: 5000,
      inviteCode: "ATL999",
      syndicUid: OUTSIDER_UID,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("buildings").doc(OTHER_BUILDING_ID).collection("members").doc(OUTSIDER_UID).set({
      uid: OUTSIDER_UID,
      displayName: "عمر",
      apartmentId: "apt_1",
      apartmentLabel: "طابق 1 - شقة 1",
      occupancyType: "OWNER",
      role: "SYNDIC",
      status: "APPROVED",
      createdAt: ts,
      updatedAt: ts,
    });
  });
}

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

test("Unauthenticated users cannot read or write any collection", async () => {
  await seedBuildingAndMembers();
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("buildings").doc(BUILDING_ID).get());
  await assertFails(unauth.collection("invites").doc("YSM482").get());
  await assertFails(unauth.collection("buildings").doc(BUILDING_ID).collection("expenses").get());
  await assertFails(unauth.collection("users").doc(SYNDIC_UID).get());
});

test("Cross-building isolation: outsider cannot read or list another building's data", async () => {
  await seedBuildingAndMembers();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  await assertFails(outsiderDb.collection("buildings").doc(BUILDING_ID).get());
  await assertFails(outsiderDb.collection("buildings").doc(BUILDING_ID).collection("apartments").get());
  await assertFails(outsiderDb.collection("buildings").doc(BUILDING_ID).collection("payments").get());
  await assertFails(outsiderDb.collection("buildings").doc(BUILDING_ID).collection("expenses").get());
  await assertFails(outsiderDb.collection("buildings").doc(BUILDING_ID).collection("issues").get());
  await assertFails(outsiderDb.collection("buildings").doc(BUILDING_ID).collection("announcements").get());
  await assertFails(outsiderDb.collection("buildings").doc(BUILDING_ID).collection("polls").get());
  await assertFails(outsiderDb.collection("buildings").doc(BUILDING_ID).collection("contacts").get());
  // Invites cannot be listed (no scraping), only fetched by exact 6-char code
  await assertFails(outsiderDb.collection("invites").get());
  await assertSucceeds(outsiderDb.collection("invites").doc("YSM482").get());
});

test("User profile and memberships are strictly isolated per user", async () => {
  const syndicDb = testEnv.authenticatedContext(SYNDIC_UID).firestore();
  const residentDb = testEnv.authenticatedContext(RESIDENT_UID).firestore();
  const ts = pastTs();

  await assertSucceeds(
    syndicDb.collection("users").doc(SYNDIC_UID).set({
      userId: SYNDIC_UID,
      displayName: "ياسين",
      email: "yassine@example.com",
      preferredLang: "AR",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  // Resident cannot read or write Syndic's private profile
  await assertFails(residentDb.collection("users").doc(SYNDIC_UID).get());
  await assertFails(
    residentDb.collection("users").doc(SYNDIC_UID).collection("memberships").doc(BUILDING_ID).get()
  );
  // Shadow field or future timestamp rejected
  await assertFails(
    residentDb.collection("users").doc(RESIDENT_UID).set({
      userId: RESIDENT_UID,
      displayName: "كريم",
      preferredLang: "AR",
      isAdmin: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    residentDb.collection("users").doc(RESIDENT_UID).set({
      userId: RESIDENT_UID,
      displayName: "كريم",
      preferredLang: "AR",
      createdAt: futureTs(),
      updatedAt: ts,
    })
  );
});

test("Join request & RBAC approval gate prevents privilege escalation", async () => {
  await seedBuildingAndMembers();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  const syndicDb = testEnv.authenticatedContext(SYNDIC_UID).firestore();
  const ts = pastTs();

  // Outsider cannot self-assign APPROVED or SYNDIC role on join
  await assertFails(
    outsiderDb.collection("buildings").doc(BUILDING_ID).collection("members").doc(OUTSIDER_UID).set({
      uid: OUTSIDER_UID,
      displayName: "عمر",
      apartmentLabel: "طابق 3 - شقة 6",
      occupancyType: "OWNER",
      role: "RESIDENT",
      status: "APPROVED",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    outsiderDb.collection("buildings").doc(BUILDING_ID).collection("members").doc(OUTSIDER_UID).set({
      uid: OUTSIDER_UID,
      displayName: "عمر",
      apartmentLabel: "طابق 3 - شقة 6",
      occupancyType: "OWNER",
      role: "SYNDIC",
      status: "PENDING",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid PENDING join request succeeds
  await assertSucceeds(
    outsiderDb.collection("buildings").doc(BUILDING_ID).collection("members").doc(OUTSIDER_UID).set({
      uid: OUTSIDER_UID,
      displayName: "عمر",
      apartmentLabel: "طابق 3 - شقة 6",
      occupancyType: "OWNER",
      role: "RESIDENT",
      status: "PENDING",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Pending member cannot read building expenses yet
  await assertFails(outsiderDb.collection("buildings").doc(BUILDING_ID).collection("expenses").get());

  // Outsider cannot self-approve
  await assertFails(
    outsiderDb.collection("buildings").doc(BUILDING_ID).collection("members").doc(OUTSIDER_UID).update({
      status: "APPROVED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Syndic approves join request
  await assertSucceeds(
    syndicDb.collection("buildings").doc(BUILDING_ID).collection("members").doc(OUTSIDER_UID).update({
      status: "APPROVED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Now approved member can read building expenses
  await assertSucceeds(outsiderDb.collection("buildings").doc(BUILDING_ID).collection("expenses").get());
});

test("Financial transparency: Syndic/Deputy record payments & expenses; Residents read-only", async () => {
  await seedBuildingAndMembers();
  const syndicDb = testEnv.authenticatedContext(SYNDIC_UID).firestore();
  const deputyDb = testEnv.authenticatedContext(DEPUTY_UID).firestore();
  const residentDb = testEnv.authenticatedContext(RESIDENT_UID).firestore();
  const ts = pastTs();

  // Resident cannot create apartment, payment, or expense
  await assertFails(
    residentDb.collection("buildings").doc(BUILDING_ID).collection("payments").doc("apt_3_202611").set({
      apartmentId: "apt_3",
      apartmentLabel: "طابق 2 - شقة 3",
      monthKey: "2026-11",
      amountMad: 250,
      status: "PAID",
      paymentMethod: "CASH",
      recordedByUid: RESIDENT_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    residentDb.collection("buildings").doc(BUILDING_ID).collection("expenses").doc("exp_1").set({
      title: "تنظيف الدرج",
      category: "CLEANING",
      amountMad: 400,
      expenseDateLabel: "2026-11-10",
      invoicePhotoBase64: "data:image/jpeg;base64,/9j/4AAQ",
      recordedByUid: RESIDENT_UID,
      recordedByName: "كريم",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Deputy can record payment
  await assertSucceeds(
    deputyDb.collection("buildings").doc(BUILDING_ID).collection("payments").doc("apt_3_202611").set({
      apartmentId: "apt_3",
      apartmentLabel: "طابق 2 - شقة 3",
      monthKey: "2026-11",
      amountMad: 250,
      status: "PAID",
      payerName: "كريم الساكن",
      paymentMethod: "CASH",
      receiptNumber: "REC-202611-03",
      paidAtLabel: "2026-11-12",
      recordedByUid: DEPUTY_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Syndic expense requires non-empty invoicePhotoBase64
  await assertFails(
    syndicDb.collection("buildings").doc(BUILDING_ID).collection("expenses").doc("exp_missing_img").set({
      title: "إصلاح المصعد",
      category: "ELEVATOR",
      amountMad: 1200,
      expenseDateLabel: "2026-11-15",
      invoicePhotoBase64: "",
      recordedByUid: SYNDIC_UID,
      recordedByName: "ياسين",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    syndicDb.collection("buildings").doc(BUILDING_ID).collection("expenses").doc("exp_1").set({
      title: "إصلاح المصعد",
      category: "ELEVATOR",
      amountMad: 1200,
      vendorName: "Atlas Ascenseurs",
      expenseDateLabel: "2026-11-15",
      invoicePhotoBase64: "/9j/4AAQSkZJRg==",
      recordedByUid: SYNDIC_UID,
      recordedByName: "ياسين السانديك",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Resident can read payments and expenses
  await assertSucceeds(residentDb.collection("buildings").doc(BUILDING_ID).collection("payments").get());
  await assertSucceeds(residentDb.collection("buildings").doc(BUILDING_ID).collection("expenses").get());
});

test("Issues, upvotes, comments, announcements, and polls enforce RBAC", async () => {
  await seedBuildingAndMembers();
  const syndicDb = testEnv.authenticatedContext(SYNDIC_UID).firestore();
  const residentDb = testEnv.authenticatedContext(RESIDENT_UID).firestore();
  const ts = pastTs();

  // Resident submits issue (must start UNDER_REVIEW with upvoteCount == 0)
  await assertSucceeds(
    residentDb.collection("buildings").doc(BUILDING_ID).collection("issues").doc("iss_1").set({
      title: "تسرب ماء في المرآب",
      description: "يوجد تسرب مستمر قرب العمود B",
      category: "PLUMBING",
      urgency: "URGENT",
      status: "UNDER_REVIEW",
      authorUid: RESIDENT_UID,
      authorName: "كريم الساكن",
      apartmentLabel: "طابق 2 - شقة 3",
      upvoteCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Resident cannot change issue status directly
  await assertFails(
    residentDb.collection("buildings").doc(BUILDING_ID).collection("issues").doc("iss_1").update({
      status: "RESOLVED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Resident can upvote (+1) and add upvote doc
  await assertSucceeds(
    residentDb.collection("buildings").doc(BUILDING_ID).collection("issues").doc("iss_1").collection("upvotes").doc(RESIDENT_UID).set({
      uid: RESIDENT_UID,
      createdAt: ts,
    })
  );
  await assertSucceeds(
    residentDb.collection("buildings").doc(BUILDING_ID).collection("issues").doc("iss_1").update({
      upvoteCount: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Syndic updates issue status & repairCostNote
  await assertSucceeds(
    syndicDb.collection("buildings").doc(BUILDING_ID).collection("issues").doc("iss_1").update({
      status: "IN_PROGRESS",
      repairCostNote: "تم استدعاء الرصاص - التقدير 450 درهم",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Resident cannot create poll or announcement
  await assertFails(
    residentDb.collection("buildings").doc(BUILDING_ID).collection("announcements").doc("ann_1").set({
      title: "إعلان",
      body: "محتوى",
      category: "GENERAL",
      pinned: true,
      authorUid: RESIDENT_UID,
      authorName: "كريم",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Syndic creates poll, resident votes
  await assertSucceeds(
    syndicDb.collection("buildings").doc(BUILDING_ID).collection("polls").doc("poll_1").set({
      question: "هل توافقون على صباغة الواجهة؟",
      options: ["نعم، أوافق", "لا أوافق", "تأجيل للجمع العام"],
      closed: false,
      authorUid: SYNDIC_UID,
      authorName: "ياسين السانديك",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    residentDb.collection("buildings").doc(BUILDING_ID).collection("polls").doc("poll_1").collection("votes").doc(RESIDENT_UID).set({
      uid: RESIDENT_UID,
      optionIndex: 0,
      createdAt: ts,
    })
  );
});

test("Kotlin createBuilding sequence succeeds against rules", async () => {
  const syndicDb = testEnv.authenticatedContext(SYNDIC_UID).firestore();
  const ts = pastTs();
  const bId = "bld_new_99";
  const bRef = syndicDb.collection("buildings").doc(bId);

  await assertSucceeds(
    bRef.set({
      name: "إقامة النخيل",
      city: "مراكش",
      neighborhood: "جليز",
      floorCount: 4,
      apartmentCount: 8,
      defaultMonthlyFee: 300,
      reserveFundBalance: 4000,
      inviteCode: "NKL246",
      syndicUid: SYNDIC_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    bRef.collection("members").doc(SYNDIC_UID).set({
      uid: SYNDIC_UID,
      displayName: "ياسين",
      apartmentId: "apt_1",
      apartmentLabel: "طابق 1 - شقة 1",
      occupancyType: "OWNER",
      role: "SYNDIC",
      status: "APPROVED",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  const batch = syndicDb.batch();
  batch.set(syndicDb.collection("invites").doc("NKL246"), {
    buildingId: bId,
    buildingName: "إقامة النخيل",
    city: "مراكش",
    syndicUid: SYNDIC_UID,
    active: true,
    createdAt: ts,
    updatedAt: ts,
  });
  batch.set(syndicDb.collection("users").doc(SYNDIC_UID).collection("memberships").doc(bId), {
    buildingId: bId,
    buildingName: "إقامة النخيل",
    city: "مراكش",
    apartmentLabel: "طابق 1 - شقة 1",
    createdAt: ts,
    updatedAt: ts,
  });
  batch.set(bRef.collection("apartments").doc("apt_1"), {
    unitNumber: "1",
    floor: 1,
    label: "طابق 1 - شقة 1",
    unitType: "APARTMENT",
    monthlyFee: 300,
    residentName: "ياسين",
    residentUid: SYNDIC_UID,
    createdAt: ts,
    updatedAt: ts,
  });
  batch.set(bRef.collection("contacts").doc("emergency_1"), {
    name: "الوقاية المدنية",
    roleTitle: "رقم طوارئ وطني",
    category: "EMERGENCY",
    phone: "15",
    createdAt: ts,
    updatedAt: ts,
  });
  await assertSucceeds(batch.commit());
});

test("Syndic assigns apartment resident with phone and posts targeted broadcast", async () => {
  await seedBuildingAndMembers();
  const syndicDb = testEnv.authenticatedContext(SYNDIC_UID).firestore();
  const residentDb = testEnv.authenticatedContext(RESIDENT_UID).firestore();
  const ts = pastTs();

  const aptRef = syndicDb.collection("buildings").doc(BUILDING_ID).collection("apartments").doc("apt_2");
  await assertSucceeds(
    aptRef.set({
      unitNumber: "2",
      floor: 1,
      label: "طابق 1 - شقة 2",
      unitType: "APARTMENT",
      monthlyFee: 250,
      residentName: "سلمى الفاسي",
      residentPhone: "0661122334",
      occupancyType: "TENANT",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Resident cannot edit apartment residentPhone
  await assertFails(
    residentDb.collection("buildings").doc(BUILDING_ID).collection("apartments").doc("apt_2").update({
      residentPhone: "0600000000",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Syndic updates resident phone and monthly fee
  await assertSucceeds(
    aptRef.update({
      residentName: "سلمى الفاسي",
      residentPhone: "0661998877",
      occupancyType: "OWNER",
      monthlyFee: 300,
      unitType: "DUPLEX",
      label: "دوبلكس 2",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Syndic posts targeted announcement for UNPAID residents
  await assertSucceeds(
    syndicDb.collection("buildings").doc(BUILDING_ID).collection("announcements").doc("ann_unpaid").set({
      title: "تذكير بواجب السانديك",
      body: "المرجو أداء واجب هذا الشهر قبل يوم 10.",
      category: "GENERAL",
      targetAudience: "UNPAID",
      pinned: true,
      authorUid: SYNDIC_UID,
      authorName: "ياسين السانديك",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Building works (A3mal): Syndic/Deputy manage work orders; Residents read-only", async () => {
  await seedBuildingAndMembers();
  const syndicDb = testEnv.authenticatedContext(SYNDIC_UID).firestore();
  const residentDb = testEnv.authenticatedContext(RESIDENT_UID).firestore();
  const ts = pastTs();

  // Resident cannot create work order
  await assertFails(
    residentDb.collection("buildings").doc(BUILDING_ID).collection("works").doc("work_1").set({
      title: "صباغة الدرج",
      category: "RENOVATION",
      contractorName: "معلم حسن",
      contractorPhone: "0661112233",
      estimatedCostMad: 2800,
      dueDateLabel: "2026-12-01",
      status: "PLANNED",
      authorUid: RESIDENT_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Syndic creates work order and advances status
  const workRef = syndicDb.collection("buildings").doc(BUILDING_ID).collection("works").doc("work_1");
  await assertSucceeds(
    workRef.set({
      title: "تنظيف خزان الماء وصيانة المضخة",
      category: "PLUMBING",
      contractorName: "شركة النقاء",
      contractorPhone: "0661556677",
      estimatedCostMad: 1500,
      dueDateLabel: "2026-11-30",
      status: "PLANNED",
      notes: "تغيير الفلتر الرئيسي",
      authorUid: SYNDIC_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    workRef.update({
      status: "IN_PROGRESS",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Resident can read building works
  await assertSucceeds(residentDb.collection("buildings").doc(BUILDING_ID).collection("works").get());
});

test("PreApprovedPhone enables Sokan invite-link auto-approval and blocks arbitrary self-approval", async () => {
  await seedBuildingAndMembers();
  const syndicDb = testEnv.authenticatedContext(SYNDIC_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  const ts = pastTs();
  const validPhoneKey = "212661998877";
  const fakePhoneKey = "212600000000";

  // Syndic registers pre-approved phone for unit 4
  await assertSucceeds(
    syndicDb.collection("buildings").doc(BUILDING_ID).collection("preApprovedPhones").doc(validPhoneKey).set({
      phoneKey: validPhoneKey,
      apartmentId: "apt_4",
      apartmentLabel: "طابق 2 - شقة 4",
      residentName: "عمر",
      occupancyType: "OWNER",
      addedByUid: SYNDIC_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Outsider with unapproved phone cannot auto-approve
  await assertFails(
    outsiderDb.collection("buildings").doc(BUILDING_ID).collection("members").doc(OUTSIDER_UID).set({
      uid: OUTSIDER_UID,
      displayName: "عمر",
      apartmentId: "apt_4",
      apartmentLabel: "طابق 2 - شقة 4",
      occupancyType: "OWNER",
      role: "RESIDENT",
      status: "APPROVED",
      phone: "0600000000",
      phoneKey: fakePhoneKey,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Outsider cannot scrape list of preApprovedPhones before approval
  await assertFails(outsiderDb.collection("buildings").doc(BUILDING_ID).collection("preApprovedPhones").get());

  // Outsider with matching pre-approved phoneKey is automatically APPROVED
  await assertSucceeds(
    outsiderDb.collection("buildings").doc(BUILDING_ID).collection("members").doc(OUTSIDER_UID).set({
      uid: OUTSIDER_UID,
      displayName: "عمر",
      apartmentId: "apt_4",
      apartmentLabel: "طابق 2 - شقة 4",
      occupancyType: "OWNER",
      role: "RESIDENT",
      status: "APPROVED",
      phone: "0661998877",
      phoneKey: validPhoneKey,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Live BuildingNotifications & ResidentAccount recovery enforce strict zero-trust", async () => {
  await seedBuildingAndMembers();
  const syndicDb = testEnv.authenticatedContext(SYNDIC_UID).firestore();
  const residentDb = testEnv.authenticatedContext(RESIDENT_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  const ts = pastTs();
  const shaHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

  // Live notification creation & read tracking
  const notifRef = syndicDb.collection("buildings").doc(BUILDING_ID).collection("notifications").doc("notif_1");
  await assertSucceeds(
    notifRef.set({
      title: "إعلان جديد",
      body: "اجتماع الجمع العام غداً",
      type: "ANNOUNCEMENT",
      senderUid: SYNDIC_UID,
      senderName: "ياسين السانديك",
      readByUids: [SYNDIC_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(outsiderDb.collection("buildings").doc(BUILDING_ID).collection("notifications").get());
  await assertSucceeds(residentDb.collection("buildings").doc(BUILDING_ID).collection("notifications").get());
  await assertSucceeds(
    residentDb.collection("buildings").doc(BUILDING_ID).collection("notifications").doc("notif_1").update({
      readByUids: [SYNDIC_UID, RESIDENT_UID],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // ResidentAccount creation (no listing allowed) + recovery transfer
  const phoneKey = "212663556677";
  await assertFails(residentDb.collection("residentAccounts").get());
  await assertSucceeds(
    residentDb.collection("residentAccounts").doc(phoneKey).set({
      phoneKey,
      displayPhone: "0663556677",
      displayName: "كريم الساكن",
      passwordHash: shaHash,
      recoveryCodeHash: shaHash,
      ownerUid: RESIDENT_UID,
      buildingIds: [BUILDING_ID],
      apartmentLabels: ["طابق 2 - شقة 3"],
      createdAt: ts,
      updatedAt: ts,
    })
  );
  // Device change recovery transfer with recoveryVerifiedAt succeeds for verified new UID
  await assertSucceeds(
    outsiderDb.collection("residentAccounts").doc(phoneKey).update({
      ownerUid: OUTSIDER_UID,
      passwordHash: shaHash,
      recoveryVerifiedAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

