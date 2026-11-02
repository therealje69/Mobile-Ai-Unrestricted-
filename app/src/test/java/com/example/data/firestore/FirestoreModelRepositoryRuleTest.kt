package com.example.data.firestore

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class FirestoreModelRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun sampleModel(id: String = "model_${UUID.randomUUID().toString().replace("-", "")}"): DeployedModelDoc {
        return DeployedModelDoc(
            id = id,
            name = "RunPod Sovereign-70B (Abliterated)",
            provider = "RUNPOD_SERVERLESS",
            endpointUrl = "https://api.runpod.ai/v2/sovereign-llama3-70b-ablit/runsync",
            modelRepoId = "failspy/Meta-Llama-3-70B-Instruct-abliterated-v3.5",
            gpuTier = "2x NVIDIA A100 80GB SXM",
            quantization = "EXL2 6.0bpw",
            weightMode = "ORTHOGONAL_ABLITERATED",
            abliterationLayerRange = "Layers 14-29 Orthogonal Projection",
            status = "RUNNING",
            activeWorkers = 2L,
            maxWorkers = 6L,
            avgLatencyMs = 240L,
            vramUsedGb = 138L,
            vramTotalGb = 160L,
            gpuUtilizationPercent = 78L,
            requestsPerMin = 42L,
            isSelected = true
        )
    }

    @Test
    fun createAndStopStartModel_authenticatedOwner_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = FirestoreModelRepository(firestore)
        val model = sampleModel()

        val createRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createDeployedModel(model)
        }
        assertTrue(createRes.isSuccess)
        val modelId = createRes.getOrThrow()

        // Verify stopModel transitions status to STOPPED and zeroes resource utilization
        val stopRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.stopModel(modelId)
        }
        assertTrue(stopRes.isSuccess)

        val fetchedStopped = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getDeployedModelsOnce().getOrThrow().first { it.id == modelId }
        }
        assertEquals("STOPPED", fetchedStopped.status)
        assertEquals(0L, fetchedStopped.activeWorkers)
        assertEquals(0L, fetchedStopped.gpuUtilizationPercent)

        // Verify startModel transitions status back to RUNNING
        val startRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.startModel(modelId, targetWorkers = 2L, vramTotalGb = 160L)
        }
        assertTrue(startRes.isSuccess)

        val fetchedRunning = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getDeployedModelsOnce().getOrThrow().first { it.id == modelId }
        }
        assertEquals("RUNNING", fetchedRunning.status)
        assertEquals(2L, fetchedRunning.activeWorkers)
    }

    @Test
    fun observeDeployedModels_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = FirestoreModelRepository(firestore)
        val model = sampleModel()
        val modelId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createDeployedModel(model).getOrThrow()
        }

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeDeployedModels().first { list -> list.any { it.id == modelId } }
        }
        assertTrue(emitted.any { it.id == modelId })
    }

    @Test
    fun getModelById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirestoreModelRepository(firestore)
        val model = sampleModel()
        val modelId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createDeployedModel(model).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = FirestoreModelRepository(firestore)
        var caughtException: Throwable? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getModelById(aliceUid, modelId)
            }
        } catch (e: Throwable) {
            caughtException = e
        }
        val fsEx = checkNotNull(caughtException?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException for cross-user access, got $caughtException"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
    }

    @Test
    fun unauthenticatedUser_directRead_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirestoreModelRepository(firestore)
        val modelId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createDeployedModel(sampleModel()).getOrThrow()
        }

        auth.signOut()
        val unauthRepo = FirestoreModelRepository(firestore)
        var caughtException: Throwable? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                unauthRepo.getModelById(aliceUid, modelId)
            }
        } catch (e: Throwable) {
            caughtException = e
        }
        val fsEx = checkNotNull(caughtException?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException for unauthenticated access, got $caughtException"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_model_admin@test.com"
        const val BOB_EMAIL = "bob_other_user@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
