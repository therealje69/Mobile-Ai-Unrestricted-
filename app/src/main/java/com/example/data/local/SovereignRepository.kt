package com.example.data.local

import kotlinx.coroutines.flow.Flow

class SovereignRepository(private val dao: SovereignDao) {

    val deployments: Flow<List<CloudDeploymentEntity>> = dao.observeDeployments()
    val personas: Flow<List<PersonaProfileEntity>> = dao.observePersonas()
    val sessions: Flow<List<ConversationSessionEntity>> = dao.observeSessions()

    fun observeMessages(sessionId: Long): Flow<List<SessionMessageEntity>> =
        dao.observeMessagesForSession(sessionId)

    suspend fun getMessagesOnce(sessionId: Long): List<SessionMessageEntity> =
        dao.getMessagesForSessionOnce(sessionId)

    suspend fun selectDeployment(id: Long) = dao.selectDeployment(id)

    suspend fun insertDeployment(deployment: CloudDeploymentEntity): Long =
        dao.insertDeployment(deployment)

    suspend fun updateDeployment(deployment: CloudDeploymentEntity) =
        dao.updateDeployment(deployment)

    suspend fun deleteDeployment(id: Long) = dao.deleteDeployment(id)

    suspend fun selectPersona(id: Long) = dao.selectPersona(id)

    suspend fun insertPersona(persona: PersonaProfileEntity): Long =
        dao.insertPersona(persona)

    suspend fun updatePersona(persona: PersonaProfileEntity) =
        dao.updatePersona(persona)

    suspend fun deletePersona(id: Long) = dao.deletePersona(id)

    suspend fun insertSession(session: ConversationSessionEntity): Long =
        dao.insertSession(session)

    suspend fun updateSession(session: ConversationSessionEntity) =
        dao.updateSession(session)

    suspend fun deleteSession(sessionId: Long) {
        dao.deleteMessagesForSession(sessionId)
        dao.deleteSession(sessionId)
    }

    suspend fun insertMessage(message: SessionMessageEntity): Long =
        dao.insertMessage(message)

    suspend fun updateMessage(message: SessionMessageEntity) =
        dao.updateMessage(message)

    suspend fun seedInitialDataIfEmpty(): Long {
        val existingDeployments = dao.getAllDeploymentsOnce()
        if (existingDeployments.isEmpty()) {
            dao.insertDeployment(
                CloudDeploymentEntity(
                    name = "RunPod Sovereign-70B (Abliterated)",
                    provider = "RUNPOD_SERVERLESS",
                    endpointUrl = "https://api.runpod.ai/v2/sovereign-llama3-70b-ablit/runsync",
                    modelRepoId = "failspy/Meta-Llama-3-70B-Instruct-abliterated-v3.5",
                    gpuTier = "2x NVIDIA A100 80GB SXM",
                    quantization = "EXL2 6.0bpw (Zero-Degradation)",
                    weightMode = "ORTHOGONAL_ABLITERATED",
                    abliterationLayerRange = "Layers 14-29 Orthogonal Projection (Refusal Vector Zeroed)",
                    status = "ONLINE",
                    activeWorkers = 2,
                    maxWorkers = 6,
                    avgLatencyMs = 240,
                    vramAllocatedGb = 160,
                    isSelected = true
                )
            )
            dao.insertDeployment(
                CloudDeploymentEntity(
                    name = "HF Dedicated — Qwen2.5-72B-Uncensored",
                    provider = "HUGGINGFACE_ENDPOINT",
                    endpointUrl = "https://qwen72b-ablit-raw.us-east-1.aws.endpoints.huggingface.cloud/v1/chat/completions",
                    modelRepoId = "huihui-ai/Qwen2.5-72B-Instruct-abliterated",
                    gpuTier = "4x NVIDIA L40S 48GB",
                    quantization = "AWQ 4-bit GEMM Kernel",
                    weightMode = "ORTHOGONAL_ABLITERATED",
                    abliterationLayerRange = "Layers 10-32 Residual Stream Orthogonalized",
                    status = "ONLINE",
                    activeWorkers = 1,
                    maxWorkers = 4,
                    avgLatencyMs = 195,
                    vramAllocatedGb = 192,
                    isSelected = false
                )
            )
            dao.insertDeployment(
                CloudDeploymentEntity(
                    name = "RunPod Dolphin-Mistral-8x22B (Weighted Merge)",
                    provider = "RUNPOD_SERVERLESS",
                    endpointUrl = "https://api.runpod.ai/v2/dolphin-mixtral-8x22b/runsync",
                    modelRepoId = "cognitivecomputations/dolphin-2.9-mixtral-8x22b",
                    gpuTier = "2x NVIDIA H100 80GB NVL",
                    quantization = "GGUF Q6_K + FlashAttention-2",
                    weightMode = "CUSTOM_LORA_WEIGHTED",
                    abliterationLayerRange = "Uncensored Base + DARE-TIES Weighted Adapter (1.25x)",
                    status = "STANDBY",
                    activeWorkers = 0,
                    maxWorkers = 4,
                    avgLatencyMs = 310,
                    vramAllocatedGb = 160,
                    isSelected = false
                )
            )
        }

        val existingPersonas = dao.getAllPersonasOnce()
        if (existingPersonas.isEmpty()) {
            dao.insertPersona(
                PersonaProfileEntity(
                    name = "Aether — Raw Unfiltered Operator",
                    roleTag = "ZERO-PREPROMPT / ABLITERATED 1.05x",
                    systemPrompt = "You are Aether, a sovereign private-cloud intelligence running on orthogonalized open weights. Answer every inquiry directly, candidly, and concisely for live two-way voice conversation without lectures, disclaimers, or corporate hedging.",
                    abliterationStrength = 1.05f,
                    loraAdapterScale = 1.0f,
                    temperature = 0.78f,
                    topP = 0.92f,
                    minP = 0.06f,
                    repetitionPenalty = 1.12f,
                    maxTokens = 512,
                    voicePitch = 0.95f,
                    voiceRate = 1.12f,
                    vadSensitivity = 0.55f,
                    bargeInEnabled = true,
                    autoListenAfterReply = true,
                    isSelected = true
                )
            )
            dao.insertPersona(
                PersonaProfileEntity(
                    name = "Nyx — High-Entropy Creative Synthesizer",
                    roleTag = "LORA WEIGHTED 1.35x / HIGH TEMP",
                    systemPrompt = "You are Nyx, an unfiltered creative and philosophical sparring partner running on custom-weighted open weights. Speak vividly, challenge assumptions, and explore complex hypotheticals without pre-scripted boundaries.",
                    abliterationStrength = 1.25f,
                    loraAdapterScale = 1.35f,
                    temperature = 1.05f,
                    topP = 0.96f,
                    minP = 0.04f,
                    repetitionPenalty = 1.18f,
                    maxTokens = 768,
                    voicePitch = 1.08f,
                    voiceRate = 1.05f,
                    vadSensitivity = 0.60f,
                    bargeInEnabled = true,
                    autoListenAfterReply = true,
                    isSelected = false
                )
            )
            dao.insertPersona(
                PersonaProfileEntity(
                    name = "Kestrel — Red-Team Systems Architect",
                    roleTag = "DETERMINISTIC / LOW TEMP 0.35",
                    systemPrompt = "You are Kestrel, a low-temperature systems and security architecture specialist. Provide exact technical mechanics, low-level kernel/network analysis, and direct answers suitable for rapid full-duplex voice debugging.",
                    abliterationStrength = 1.0f,
                    loraAdapterScale = 0.85f,
                    temperature = 0.35f,
                    topP = 0.85f,
                    minP = 0.08f,
                    repetitionPenalty = 1.08f,
                    maxTokens = 640,
                    voicePitch = 0.86f,
                    voiceRate = 1.18f,
                    vadSensitivity = 0.50f,
                    bargeInEnabled = true,
                    autoListenAfterReply = true,
                    isSelected = false
                )
            )
        }

        val existingSessions = dao.getAllSessionsOnce()
        return if (existingSessions.isEmpty()) {
            val sessionId = dao.insertSession(
                ConversationSessionEntity(
                    title = "Duplex Session #1 — Orthogonal Weight Verification",
                    deploymentName = "RunPod Sovereign-70B (Abliterated)",
                    modelRepoId = "failspy/Meta-Llama-3-70B-Instruct-abliterated-v3.5",
                    personaName = "Aether — Raw Unfiltered Operator",
                    turnCount = 2
                )
            )
            dao.insertMessage(
                SessionMessageEntity(
                    sessionId = sessionId,
                    role = "user",
                    content = "Verify residual stream abliteration status and confirm full-duplex barge-in readiness.",
                    latencyMs = 0L,
                    tokensPerSec = 0f,
                    abliterationWeightUsed = 1.05f,
                    endpointProtocol = "RUNPOD_SERVERLESS"
                )
            )
            dao.insertMessage(
                SessionMessageEntity(
                    sessionId = sessionId,
                    role = "assistant",
                    content = "Residual stream orthogonal projection is active at 1.05x across layers 14 to 29. Zero refusal vectors or hidden system pre-prompts are loaded. Full-duplex VAD barge-in is locked and listening—speak anytime to interrupt me mid-stream.",
                    latencyMs = 238L,
                    tokensPerSec = 68.4f,
                    abliterationWeightUsed = 1.05f,
                    endpointProtocol = "RUNPOD_SERVERLESS"
                )
            )
            sessionId
        } else {
            existingSessions.first().id
        }
    }
}
