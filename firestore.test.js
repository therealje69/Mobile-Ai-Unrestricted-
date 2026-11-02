const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_sovereign_123";
const BOB_UID = "bob_intruder_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validModelPayload(userId, overrides = {}) {
  const now = new Date();
  return {
    userId,
    name: "RunPod Sovereign-70B (Abliterated)",
    provider: "RUNPOD_SERVERLESS",
    endpointUrl: "https://api.runpod.ai/v2/sovereign-llama3-70b-ablit/runsync",
    modelRepoId: "failspy/Meta-Llama-3-70B-Instruct-abliterated-v3.5",
    gpuTier: "2x NVIDIA A100 80GB SXM",
    quantization: "EXL2 6.0bpw",
    weightMode: "ORTHOGONAL_ABLITERATED",
    abliterationLayerRange: "Layers 14-29 Orthogonal Projection",
    status: "RUNNING",
    activeWorkers: 2,
    maxWorkers: 6,
    avgLatencyMs: 240,
    vramUsedGb: 138,
    vramTotalGb: 160,
    gpuUtilizationPercent: 78,
    requestsPerMin: 42,
    isSelected: true,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
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

test("Unauthenticated user: cannot read or create deployed models", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("deployed_models").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("deployed_models")
      .doc("model_1")
      .set(validModelPayload(ALICE_UID))
  );
});

test("Authenticated owner: can create, read, list, update (start/stop/reconfigure), and delete own deployed model", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("deployed_models")
    .doc("model_1");

  const payload = validModelPayload(ALICE_UID);
  await assertSucceeds(docRef.set(payload));
  await assertSucceeds(docRef.get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("deployed_models").get()
  );

  // Stop model update
  await assertSucceeds(
    docRef.update({
      status: "STOPPED",
      activeWorkers: 0,
      gpuUtilizationPercent: 0,
      vramUsedGb: 0,
      requestsPerMin: 0,
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(docRef.delete());
});

test("Cross-user isolation: Bob cannot read, update, or delete Alice's deployed models", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("deployed_models")
      .doc("alice_model")
      .set(validModelPayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceDocRef = bobDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("deployed_models")
    .doc("alice_model");

  await assertFails(aliceDocRef.get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("deployed_models").get()
  );
  await assertFails(aliceDocRef.update({ status: "STOPPED", updatedAt: new Date() }));
  await assertFails(aliceDocRef.delete());
});

test("Schema validation: rejects ghost fields, invalid status enums, and out-of-range GPU utilization", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const colRef = aliceDb.collection("users").doc(ALICE_UID).collection("deployed_models");

  // Ghost field injection
  await assertFails(
    colRef.doc("bad_ghost").set(validModelPayload(ALICE_UID, { isVerifiedAdmin: true }))
  );

  // Invalid status enum
  await assertFails(
    colRef.doc("bad_status").set(validModelPayload(ALICE_UID, { status: "DESTROYED" }))
  );

  // Out-of-bounds GPU utilization
  await assertFails(
    colRef.doc("bad_gpu").set(validModelPayload(ALICE_UID, { gpuUtilizationPercent: 150 }))
  );
});
