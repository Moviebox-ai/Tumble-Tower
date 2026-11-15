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

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function serverTime() {
  return new Date(Date.now() - 1000);
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

test("Unauthenticated user cannot read or write profiles or leaderboards", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb
      .collection("leaderboard_entries")
      .where("isPublic", "==", true)
      .where("boardType", "==", "daily")
      .get()
  );
  await assertFails(
    unauthDb.collection("leaderboard_entries").doc("entry_1").set({
      userId: ALICE_UID,
      displayName: "Alice",
      boardType: "daily",
      dateKey: "2026-11-13",
      score: 1000,
      height: 15,
      isPublic: true,
      createdAt: serverTime(),
      updatedAt: serverTime(),
    })
  );
});

test("Authenticated user can create and update own profile, but cannot access Bob's", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      coins: 500,
      gems: 20,
      xpLevel: 1,
      createdAt: serverTime(),
      updatedAt: serverTime(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coins: 750,
      updatedAt: serverTime(),
    })
  );

  // Cross-user read/write must fail
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      coins: 9999,
      updatedAt: serverTime(),
    })
  );
});

test("Shadow update with ghost field on UserProfile is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      coins: 500,
      gems: 20,
      xpLevel: 1,
      isAdmin: true,
      createdAt: serverTime(),
      updatedAt: serverTime(),
    })
  );
});

test("Authenticated user can manage own level_progress subcollection, isolated from others", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("level_progress")
      .doc("level_1")
      .set({
        userId: ALICE_UID,
        levelNumber: 1,
        worldId: 1,
        stars: 3,
        highScore: 4250,
        bestHeight: 18,
        completed: true,
        createdAt: serverTime(),
        updatedAt: serverTime(),
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("level_progress")
      .get()
  );

  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("level_progress")
      .get()
  );
});

test("Leaderboard enforces ownership on writes and aligned filter on list", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("leaderboard_entries").doc("alice_daily").set({
      userId: ALICE_UID,
      displayName: "Alice",
      avatarId: "avatar_1",
      boardType: "daily",
      dateKey: "2026-11-13",
      score: 8420,
      height: 24,
      accuracyPercent: 92,
      isPublic: true,
      createdAt: serverTime(),
      updatedAt: serverTime(),
    })
  );

  // Aligned query succeeds
  await assertSucceeds(
    bobDb
      .collection("leaderboard_entries")
      .where("isPublic", "==", true)
      .where("boardType", "==", "daily")
      .get()
  );

  // Unfiltered query fails
  await assertFails(bobDb.collection("leaderboard_entries").get());

  // Bob cannot update or delete Alice's leaderboard entry
  await assertFails(
    bobDb.collection("leaderboard_entries").doc("alice_daily").update({
      score: 1,
      updatedAt: serverTime(),
    })
  );
  await assertFails(
    bobDb.collection("leaderboard_entries").doc("alice_daily").delete()
  );
});
