package com.example.data.firestore

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@IgnoreExtraProperties
data class DeployedModelDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val provider: String = "RUNPOD_SERVERLESS",
    val endpointUrl: String = "",
    val modelRepoId: String = "",
    val gpuTier: String = "",
    val quantization: String = "",
    val weightMode: String = "ORTHOGONAL_ABLITERATED",
    val abliterationLayerRange: String = "",
    val status: String = "RUNNING", // "RUNNING", "STOPPED", "WARMING_UP", "SCALING"
    val activeWorkers: Long = 1L,
    val maxWorkers: Long = 4L,
    val avgLatencyMs: Long = 220L,
    val vramUsedGb: Long = 64L,
    val vramTotalGb: Long = 80L,
    val gpuUtilizationPercent: Long = 72L,
    val requestsPerMin: Long = 28L,
    val isSelected: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> {
        return mapOf(
            "userId" to ownerUid,
            "name" to name,
            "provider" to provider,
            "endpointUrl" to endpointUrl,
            "modelRepoId" to modelRepoId,
            "gpuTier" to gpuTier,
            "quantization" to quantization,
            "weightMode" to weightMode,
            "abliterationLayerRange" to abliterationLayerRange,
            "status" to status,
            "activeWorkers" to activeWorkers,
            "maxWorkers" to maxWorkers,
            "avgLatencyMs" to avgLatencyMs,
            "vramUsedGb" to vramUsedGb,
            "vramTotalGb" to vramTotalGb,
            "gpuUtilizationPercent" to gpuUtilizationPercent,
            "requestsPerMin" to requestsPerMin,
            "isSelected" to isSelected,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

class FirestoreModelRepository(
    private val db: FirebaseFirestore
) {
    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userModelsCollection(uid: String) =
        db.collection("users").document(uid).collection("deployed_models")

    fun observeDeployedModels(): Flow<List<DeployedModelDoc>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/deployed_models"
        emitAll(
            userModelsCollection(uid)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(
                            DeployedModelDoc::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }.sortedWith(
                        compareByDescending<DeployedModelDoc> { it.isSelected }
                            .thenByDescending { it.status == "RUNNING" }
                            .thenBy { it.name }
                    )
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getDeployedModelsOnce(): Result<List<DeployedModelDoc>> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "users/unauthenticated/deployed_models")
            return Result.failure(e)
        }
        val path = "users/$uid/deployed_models"
        return try {
            val snap = userModelsCollection(uid).whereEqualTo("userId", uid).get().await()
            val models = snap.documents.mapNotNull { doc ->
                doc.toObject(
                    DeployedModelDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            Result.success(models)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun getModelById(targetUserId: String, modelId: String): Result<DeployedModelDoc> {
        val path = "users/$targetUserId/deployed_models/$modelId"
        return try {
            val snap = userModelsCollection(targetUserId).document(modelId).get().await()
            val model = snap.toObject(
                DeployedModelDoc::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: throw NoSuchElementException("Model $modelId not found")
            Result.success(model)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            throw e
        }
    }

    suspend fun createDeployedModel(model: DeployedModelDoc): Result<String> {
        val uid = requireUserId()
        val colRef = userModelsCollection(uid)
        val docRef = if (model.id.isNotBlank()) colRef.document(model.id) else colRef.document()
        return try {
            docRef.set(model.toCreateMap(uid)).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun startModel(modelId: String, targetWorkers: Long = 2L, vramTotalGb: Long = 160L): Result<Unit> {
        val uid = requireUserId()
        val docRef = userModelsCollection(uid).document(modelId)
        val usedVram = ((vramTotalGb * 0.85).toLong()).coerceIn(8L, vramTotalGb)
        val updates = mapOf(
            "status" to "RUNNING",
            "activeWorkers" to targetWorkers.coerceAtLeast(1L),
            "vramUsedGb" to usedVram,
            "gpuUtilizationPercent" to 76L,
            "requestsPerMin" to 38L,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun stopModel(modelId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = userModelsCollection(uid).document(modelId)
        val updates = mapOf(
            "status" to "STOPPED",
            "activeWorkers" to 0L,
            "vramUsedGb" to 0L,
            "gpuUtilizationPercent" to 0L,
            "requestsPerMin" to 0L,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun reconfigureModel(
        modelId: String,
        name: String,
        provider: String,
        endpointUrl: String,
        modelRepoId: String,
        gpuTier: String,
        quantization: String,
        weightMode: String,
        abliterationLayerRange: String,
        maxWorkers: Long,
        vramTotalGb: Long
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = userModelsCollection(uid).document(modelId)
        val updates = mapOf(
            "name" to name.trim().take(120),
            "provider" to provider,
            "endpointUrl" to endpointUrl.trim().take(350),
            "modelRepoId" to modelRepoId.trim().take(220),
            "gpuTier" to gpuTier.trim().take(120),
            "quantization" to quantization.trim().take(120),
            "weightMode" to weightMode,
            "abliterationLayerRange" to abliterationLayerRange.trim().take(250),
            "maxWorkers" to maxWorkers.coerceIn(1L, 64L),
            "vramTotalGb" to vramTotalGb.coerceIn(8L, 2048L),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun selectActiveModel(modelId: String, allModels: List<DeployedModelDoc>): Result<Unit> {
        val uid = requireUserId()
        return try {
            allModels.forEach { m ->
                val shouldSelect = m.id == modelId
                if (m.isSelected != shouldSelect && m.id.isNotBlank()) {
                    val docRef = userModelsCollection(uid).document(m.id)
                    docRef.update(
                        mapOf(
                            "isSelected" to shouldSelect,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid/deployed_models/$modelId")
            Result.failure(e)
        }
    }

    suspend fun deleteModel(modelId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = userModelsCollection(uid).document(modelId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun seedDefaultModelsIfEmpty() {
        val existingResult = getDeployedModelsOnce()
        val existing = existingResult.getOrNull() ?: return
        if (existing.isNotEmpty()) return

        val defaults = listOf(
            DeployedModelDoc(
                id = "sovereign_llama3_70b",
                name = "RunPod Sovereign-70B (Abliterated)",
                provider = "RUNPOD_SERVERLESS",
                endpointUrl = "https://api.runpod.ai/v2/sovereign-llama3-70b-ablit/runsync",
                modelRepoId = "failspy/Meta-Llama-3-70B-Instruct-abliterated-v3.5",
                gpuTier = "2x NVIDIA A100 80GB SXM",
                quantization = "EXL2 6.0bpw (Zero-Degradation)",
                weightMode = "ORTHOGONAL_ABLITERATED",
                abliterationLayerRange = "Layers 14-29 Orthogonal Projection (Refusal Vector Zeroed)",
                status = "RUNNING",
                activeWorkers = 2L,
                maxWorkers = 6L,
                avgLatencyMs = 235L,
                vramUsedGb = 138L,
                vramTotalGb = 160L,
                gpuUtilizationPercent = 82L,
                requestsPerMin = 46L,
                isSelected = true
            ),
            DeployedModelDoc(
                id = "hf_qwen72b_ablit",
                name = "HF Dedicated — Qwen2.5-72B-Uncensored",
                provider = "HUGGINGFACE_ENDPOINT",
                endpointUrl = "https://qwen72b-ablit-raw.us-east-1.aws.endpoints.huggingface.cloud/v1/chat/completions",
                modelRepoId = "huihui-ai/Qwen2.5-72B-Instruct-abliterated",
                gpuTier = "4x NVIDIA L40S 48GB",
                quantization = "AWQ 4-bit GEMM Kernel",
                weightMode = "ORTHOGONAL_ABLITERATED",
                abliterationLayerRange = "Layers 10-32 Residual Stream Orthogonalized",
                status = "RUNNING",
                activeWorkers = 2L,
                maxWorkers = 4L,
                avgLatencyMs = 190L,
                vramUsedGb = 156L,
                vramTotalGb = 192L,
                gpuUtilizationPercent = 68L,
                requestsPerMin = 34L,
                isSelected = false
            ),
            DeployedModelDoc(
                id = "runpod_dolphin_mixtral",
                name = "RunPod Dolphin-Mistral-8x22B (Weighted)",
                provider = "RUNPOD_SERVERLESS",
                endpointUrl = "https://api.runpod.ai/v2/dolphin-mixtral-8x22b/runsync",
                modelRepoId = "cognitivecomputations/dolphin-2.9-mixtral-8x22b",
                gpuTier = "2x NVIDIA H100 80GB NVL",
                quantization = "GGUF Q6_K + FlashAttention-2",
                weightMode = "CUSTOM_LORA_WEIGHTED",
                abliterationLayerRange = "Uncensored Base + DARE-TIES Weighted Adapter (1.25x)",
                status = "STOPPED",
                activeWorkers = 0L,
                maxWorkers = 4L,
                avgLatencyMs = 310L,
                vramUsedGb = 0L,
                vramTotalGb = 160L,
                gpuUtilizationPercent = 0L,
                requestsPerMin = 0L,
                isSelected = false
            )
        )
        defaults.forEach { createDeployedModel(it) }
    }
}
