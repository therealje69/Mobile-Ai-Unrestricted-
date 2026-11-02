# Security Specification — SovereignVoice Firestore Rules

## 1. Data Invariants
1. **Strict Multi-Tenant Ownership**: Every `DeployedModel` document under `/users/{userId}/deployed_models/{modelId}` belongs exclusively to `userId`. Only an authenticated user with `request.auth.uid == userId` may read (`get`, `list`), `create`, `update`, or `delete` documents in their subcollection.
2. **Identity Integrity**: On creation, `incoming().userId == request.auth.uid && request.auth.uid == userId`. On update, `incoming().userId == existing().userId && incoming().createdAt == existing().createdAt`.
3. **Strict Schema & Bounds Validation**: All required fields must be present with strict type, enum, and string/numeric bounds (`isValidDeployedModel`).
4. **Temporal Integrity**: `createdAt` and `updatedAt` must be Firestore `timestamp` values (`<= request.time`) populated via `FieldValue.serverTimestamp()`.

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Read**: `unauthDb. doc('/users/alice/deployed_models/m1').get()` -> REJECTED (`isSignedIn()`).
2. **Cross-User Read**: `bobDb.doc('/users/alice/deployed_models/m1').get()` -> REJECTED (`request.auth.uid == userId`).
3. **Cross-User List**: `bobDb.collection('/users/alice/deployed_models').get()` -> REJECTED (`request.auth.uid == userId`).
4. **Identity Spoofing on Create**: `aliceDb` writing `userId: "bob"` under `/users/alice/deployed_models/m1` -> REJECTED (`incoming().userId == request.auth.uid`).
5. **Shadow Update / Ghost Field Injection**: Adding `"isAdmin": true` on create or update -> REJECTED (`keys().hasOnly(...)` / `affectedKeys().hasOnly(...)`).
6. **Immutable `createdAt` Tampering**: Modifying `createdAt` during an update -> REJECTED (`incoming().createdAt == existing().createdAt`).
7. **Immutable `userId` Transfer**: Changing `userId` during an update -> REJECTED (`incoming().userId == existing().userId`).
8. **Invalid Status Enum**: Writing `status: "HACKED"` -> REJECTED (`status in ['RUNNING', 'STOPPED', 'WARMING_UP', 'SCALING']`).
9. **Invalid Provider Enum**: Writing `provider: "UNKNOWN"` -> REJECTED (`provider in ['RUNPOD_SERVERLESS', 'HUGGINGFACE_ENDPOINT']`).
10. **Out-of-Bounds Resource Utilization**: Writing `gpuUtilizationPercent: 250` or `vramUsedGb: -5` -> REJECTED (`0 <= gpuUtilizationPercent <= 100`).
11. **Oversized String DoS**: Writing a 5,000-char string into `name` or `endpointUrl` -> REJECTED (`size() <= max`).
12. **Future Timestamp Spoofing**: Writing `createdAt` in the future (`> request.time`) -> REJECTED (`<= request.time`).
