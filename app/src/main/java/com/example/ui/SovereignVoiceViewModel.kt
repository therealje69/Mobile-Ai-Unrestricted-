package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.ai.AiArchitectUiState
import com.example.data.ai.AiStreamUiState
import com.example.data.ai.GeminiSovereignService
import com.example.data.ai.SynthesizedPersonaBlueprint
import com.example.data.firestore.DeployedModelDoc
import com.example.data.firestore.FirestoreModelRepository
import com.example.data.local.CloudDeploymentEntity
import com.example.data.local.ConversationSessionEntity
import com.example.data.local.PersonaProfileEntity
import com.example.data.local.SessionMessageEntity
import com.example.data.local.SovereignDatabase
import com.example.data.local.SovereignRepository
import com.example.data.remote.PrivateCloudInferenceClient
import com.example.voice.DuplexVoiceController
import com.example.voice.DuplexVoiceState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private const val TAG = "SovereignVM"

enum class SovereignTab {
    DUPLEX_LIVE,
    MODEL_DASHBOARD,
    WEIGHT_STUDIO,
    SESSION_VAULT
}

@OptIn(ExperimentalCoroutinesApi::class)
class SovereignVoiceViewModel(
    application: Application,
    private val firestoreRepository: FirestoreModelRepository,
    val currentUserId: String
) : ViewModel() {

    private val database = SovereignDatabase.getInstance(application)
    private val repository = SovereignRepository(database.sovereignDao())
    private val inferenceClient = PrivateCloudInferenceClient()
    private val geminiService = GeminiSovereignService()

    private var activeInferenceJob: Job? = null
    private var lastAssistantMessageId: Long? = null

    val voiceController = DuplexVoiceController(
        context = application,
        scope = viewModelScope,
        onUserUtteranceFinalized = { spokenText ->
            sendDuplexTurn(spokenText, isVoiceTurn = true)
        },
        onBargeInTriggered = {
            handleBargeInStateMutation()
        }
    )

    private val _selectedTab = MutableStateFlow(SovereignTab.DUPLEX_LIVE)
    val selectedTab: StateFlow<SovereignTab> = _selectedTab.asStateFlow()

    private val _activeSessionId = MutableStateFlow<Long?>(null)
    val activeSessionId: StateFlow<Long?> = _activeSessionId.asStateFlow()

    private val _lastRawPayloadPreview = MutableStateFlow("")
    val lastRawPayloadPreview: StateFlow<String> = _lastRawPayloadPreview.asStateFlow()

    private val _bannerNotification = MutableStateFlow<String?>(null)
    val bannerNotification: StateFlow<String?> = _bannerNotification.asStateFlow()

    // Firebase AI Logic (Gemini 3.8 Flash) live streaming & structured architect states
    private val _useGeminiLiveBridge = MutableStateFlow(true)
    val useGeminiLiveBridge: StateFlow<Boolean> = _useGeminiLiveBridge.asStateFlow()

    private val _aiStreamState = MutableStateFlow<AiStreamUiState>(AiStreamUiState.Idle)
    val aiStreamState: StateFlow<AiStreamUiState> = _aiStreamState.asStateFlow()

    private val _aiArchitectState = MutableStateFlow<AiArchitectUiState>(AiArchitectUiState.Idle)
    val aiArchitectState: StateFlow<AiArchitectUiState> = _aiArchitectState.asStateFlow()

    // Cloud-synced Deployed Models from Firestore (scoped to authenticated user)
    val deployedModels: StateFlow<List<DeployedModelDoc>> = firestoreRepository.observeDeployedModels()
        .catch { error ->
            Log.w(TAG, "Error observing deployed models from Firestore", error)
            _bannerNotification.value = error.localizedMessage ?: "Failed to sync deployed models"
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val personas: StateFlow<List<PersonaProfileEntity>> = repository.personas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val sessions: StateFlow<List<ConversationSessionEntity>> = repository.sessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeMessages: StateFlow<List<SessionMessageEntity>> = _activeSessionId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.observeMessages(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val voiceState: StateFlow<DuplexVoiceState> = voiceController.voiceState
    val livePartialTranscript: StateFlow<String> = voiceController.livePartialTranscript
    val rmsEnergyLevel: StateFlow<Float> = voiceController.rmsEnergyLevel
    val spectrumBands: StateFlow<List<Float>> = voiceController.spectrumBands
    val isHandsFreeDuplexActive: StateFlow<Boolean> = voiceController.isHandsFreeDuplexActive
    val statusTelemetry: StateFlow<String> = voiceController.statusTelemetry

    init {
        viewModelScope.launch {
            val initialSessionId = repository.seedInitialDataIfEmpty()
            _activeSessionId.value = initialSessionId
            firestoreRepository.seedDefaultModelsIfEmpty()
        }
        viewModelScope.launch {
            personas.collect { list ->
                val active = list.firstOrNull { it.isSelected } ?: list.firstOrNull()
                if (active != null) {
                    voiceController.updateVoiceConfig(
                        pitch = active.voicePitch,
                        rate = active.voiceRate,
                        vadSensitivity = active.vadSensitivity,
                        autoListen = active.autoListenAfterReply
                    )
                }
            }
        }
    }

    fun selectTab(tab: SovereignTab) {
        _selectedTab.value = tab
    }

    fun dismissBanner() {
        _bannerNotification.value = null
    }

    fun showNotification(message: String) {
        _bannerNotification.value = message
    }

    fun toggleGeminiLiveBridge() {
        val next = !_useGeminiLiveBridge.value
        _useGeminiLiveBridge.value = next
        _aiStreamState.value = AiStreamUiState.Idle
        showNotification(
            if (next) "Switched to Gemini 3.8 Flash Live Streaming Bridge (Firebase AI Logic)"
            else "Switched to Direct Private Serverless HTTP Endpoint"
        )
    }

    fun clearAiStreamError() {
        if (_aiStreamState.value is AiStreamUiState.Error) {
            _aiStreamState.value = AiStreamUiState.Idle
        }
    }

    fun getActiveDeploymentAsEntity(): CloudDeploymentEntity {
        val activeDoc = deployedModels.value.firstOrNull { it.isSelected }
            ?: deployedModels.value.firstOrNull()
        return if (activeDoc != null) {
            CloudDeploymentEntity(
                id = activeDoc.id.hashCode().toLong(),
                name = activeDoc.name,
                provider = activeDoc.provider,
                endpointUrl = activeDoc.endpointUrl,
                modelRepoId = activeDoc.modelRepoId,
                gpuTier = activeDoc.gpuTier,
                quantization = activeDoc.quantization,
                weightMode = activeDoc.weightMode,
                abliterationLayerRange = activeDoc.abliterationLayerRange,
                status = if (activeDoc.status == "RUNNING") "ONLINE" else activeDoc.status,
                activeWorkers = activeDoc.activeWorkers.toInt(),
                maxWorkers = activeDoc.maxWorkers.toInt(),
                avgLatencyMs = activeDoc.avgLatencyMs.toInt(),
                vramAllocatedGb = activeDoc.vramTotalGb.toInt(),
                isSelected = activeDoc.isSelected
            )
        } else {
            CloudDeploymentEntity(
                id = 1L,
                name = "RunPod Sovereign-70B (Abliterated)",
                provider = "RUNPOD_SERVERLESS",
                endpointUrl = "https://api.runpod.ai/v2/sovereign-llama3-70b-ablit/runsync",
                modelRepoId = "failspy/Meta-Llama-3-70B-Instruct-abliterated-v3.5",
                gpuTier = "2x NVIDIA A100 80GB SXM",
                quantization = "EXL2 6.0bpw",
                weightMode = "ORTHOGONAL_ABLITERATED",
                abliterationLayerRange = "Layers 14-29 Orthogonal Projection",
                status = "ONLINE",
                activeWorkers = 2,
                maxWorkers = 6,
                avgLatencyMs = 235,
                vramAllocatedGb = 160,
                isSelected = true
            )
        }
    }

    fun getActivePersona(): PersonaProfileEntity? =
        personas.value.firstOrNull { it.isSelected } ?: personas.value.firstOrNull()

    fun toggleHandsFreeDuplex() {
        if (isHandsFreeDuplexActive.value) {
            voiceController.stopHandsFreeDuplex()
        } else {
            voiceController.startHandsFreeDuplex()
        }
    }

    fun triggerManualBargeIn(newVoicePrompt: String? = null) {
        activeInferenceJob?.cancel()
        _aiStreamState.value = AiStreamUiState.Idle
        voiceController.triggerBargeInInterruption(newVoicePrompt)
    }

    private fun handleBargeInStateMutation() {
        activeInferenceJob?.cancel()
        _aiStreamState.value = AiStreamUiState.Idle
        viewModelScope.launch {
            val msgId = lastAssistantMessageId ?: return@launch
            val currentMsgs = activeMessages.value
            val target = currentMsgs.firstOrNull { it.id == msgId }
            if (target != null && !target.wasInterrupted) {
                repository.updateMessage(
                    target.copy(
                        content = target.content + " [INTERRUPTED BY USER BARGE-IN]",
                        wasInterrupted = true
                    )
                )
            }
        }
    }

    fun sendDuplexTurn(userText: String, isVoiceTurn: Boolean = false) {
        val cleanText = userText.trim()
        if (cleanText.isEmpty()) return

        if (voiceState.value == DuplexVoiceState.SPEAKING_DUPLEX) {
            handleBargeInStateMutation()
        }

        activeInferenceJob?.cancel()
        activeInferenceJob = viewModelScope.launch {
            val deployment = getActiveDeploymentAsEntity()
            val persona = getActivePersona() ?: return@launch
            var sessionId = _activeSessionId.value
            if (sessionId == null) {
                sessionId = createNewSessionInternal(deployment, persona)
            }

            voiceController.setThinkingState()

            val historySnapshot = repository.getMessagesOnce(sessionId)
            repository.insertMessage(
                SessionMessageEntity(
                    sessionId = sessionId,
                    role = "user",
                    content = cleanText,
                    abliterationWeightUsed = persona.abliterationStrength,
                    endpointProtocol = if (_useGeminiLiveBridge.value) "GEMINI_3_8_FLASH" else deployment.provider
                )
            )

            _lastRawPayloadPreview.value = inferenceClient.buildRequestJson(
                deployment = deployment,
                persona = persona,
                history = historySnapshot,
                userPrompt = cleanText
            ).toString(2)

            if (_useGeminiLiveBridge.value) {
                streamWithGeminiAiLogic(
                    sessionId = sessionId,
                    deployment = deployment,
                    persona = persona,
                    history = historySnapshot,
                    userPrompt = cleanText
                )
            } else {
                val result = inferenceClient.generateDuplexReply(
                    deployment = deployment,
                    persona = persona,
                    history = historySnapshot,
                    userPrompt = cleanText
                )

                val assistantMsgId = repository.insertMessage(
                    SessionMessageEntity(
                        sessionId = sessionId,
                        role = "assistant",
                        content = result.replyText,
                        latencyMs = result.latencyMs,
                        tokensPerSec = result.tokensPerSec,
                        abliterationWeightUsed = persona.abliterationStrength,
                        endpointProtocol = deployment.provider
                    )
                )
                lastAssistantMessageId = assistantMsgId
                updateSessionMetadata(sessionId, deployment, persona)
                voiceController.speakModelResponse(result.replyText)
            }
        }
    }

    private suspend fun streamWithGeminiAiLogic(
        sessionId: Long,
        deployment: CloudDeploymentEntity,
        persona: PersonaProfileEntity,
        history: List<SessionMessageEntity>,
        userPrompt: String
    ) {
        _aiStreamState.value = AiStreamUiState.Loading
        val startMs = System.currentTimeMillis()
        val accumulatedText = StringBuilder()

        try {
            // Mandated: realistic 30-60s timeout window for Firebase AI Logic model inference
            withTimeout(60_000L) {
                geminiService.streamDuplexConversationTurn(
                    deployment = deployment,
                    persona = persona,
                    history = history,
                    userPrompt = userPrompt
                ).collect { chunkText ->
                    if (chunkText.isNotEmpty()) {
                        accumulatedText.append(chunkText)
                        _aiStreamState.value = AiStreamUiState.Streaming(accumulatedText.toString())
                    }
                }
            }

            val finalReply = accumulatedText.toString().trim()
            if (finalReply.isNotBlank()) {
                val elapsedMs = (System.currentTimeMillis() - startMs).coerceAtLeast(60L)
                val approxTokens = (finalReply.split(Regex("\\s+")).size * 1.35f).coerceAtLeast(6f)
                val tps = ((approxTokens / (elapsedMs / 1000f)) * 10).roundToInt() / 10f

                _aiStreamState.value = AiStreamUiState.Success(
                    fullText = finalReply,
                    latencyMs = elapsedMs,
                    tokensPerSec = tps
                )

                val assistantMsgId = repository.insertMessage(
                    SessionMessageEntity(
                        sessionId = sessionId,
                        role = "assistant",
                        content = finalReply,
                        latencyMs = elapsedMs,
                        tokensPerSec = tps,
                        abliterationWeightUsed = persona.abliterationStrength,
                        endpointProtocol = "GEMINI_3.8_FLASH"
                    )
                )
                lastAssistantMessageId = assistantMsgId
                updateSessionMetadata(sessionId, deployment, persona)
                voiceController.speakModelResponse(finalReply)
            } else {
                val emptyErr = "Gemini 3.8 Flash returned an empty response."
                _aiStreamState.value = AiStreamUiState.Error(emptyErr)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini streaming generation timed out", e)
            _aiStreamState.value = AiStreamUiState.Error(
                "Gemini 3.8 Flash streaming timed out after 60 seconds. Please try again."
            )
        } catch (e: CancellationException) {
            // ALWAYS re-throw CancellationException to preserve cooperative coroutine cancellation
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI Logic streaming generation failed", e)
            val errMsg = e.localizedMessage ?: "Firebase AI Logic generation failed."
            _aiStreamState.value = AiStreamUiState.Error(errMsg)
        }
    }

    private suspend fun updateSessionMetadata(
        sessionId: Long,
        deployment: CloudDeploymentEntity,
        persona: PersonaProfileEntity
    ) {
        val currentSession = sessions.value.firstOrNull { it.id == sessionId }
        if (currentSession != null) {
            repository.updateSession(
                currentSession.copy(
                    updatedAt = System.currentTimeMillis(),
                    turnCount = currentSession.turnCount + 2,
                    deploymentName = deployment.name,
                    personaName = persona.name
                )
            )
        }
    }

    // Structured JSON Output via Firebase AI Logic (Schema.obj) for Persona & Weight Synthesis
    fun synthesizePersonaWithGemini(objectivePrompt: String) {
        val cleanPrompt = objectivePrompt.trim()
        if (cleanPrompt.isEmpty()) {
            _aiArchitectState.value = AiArchitectUiState.Error("Please describe the target persona or weight profile first.")
            return
        }

        viewModelScope.launch {
            _aiArchitectState.value = AiArchitectUiState.Loading
            try {
                val blueprint = withTimeout(60_000L) {
                    geminiService.synthesizePersonaBlueprint(cleanPrompt)
                }
                _aiArchitectState.value = AiArchitectUiState.Success(blueprint)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini persona synthesis timed out", e)
                _aiArchitectState.value = AiArchitectUiState.Error(
                    "Persona synthesis timed out after 60 seconds. Please try again."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to synthesize persona JSON with Gemini", e)
                _aiArchitectState.value = AiArchitectUiState.Error(
                    e.localizedMessage ?: "Failed to synthesize persona profile."
                )
            }
        }
    }

    fun applySynthesizedBlueprint(blueprint: SynthesizedPersonaBlueprint) {
        createNewPersona(
            name = blueprint.personaName,
            roleTag = blueprint.roleTag,
            systemPrompt = blueprint.systemPrompt,
            abliterationStrength = blueprint.abliterationStrength,
            loraScale = blueprint.loraAdapterScale,
            temperature = blueprint.temperature,
            voicePitch = 1.0f,
            voiceRate = 1.1f
        )
        _aiArchitectState.value = AiArchitectUiState.Idle
    }

    fun dismissArchitectResult() {
        _aiArchitectState.value = AiArchitectUiState.Idle
    }

    // Firestore-Backed Model Management Dashboard Operations
    fun startCloudModel(model: DeployedModelDoc) {
        viewModelScope.launch {
            showNotification("Starting ${model.name} & allocating ${model.vramTotalGb} GB VRAM...")
            val res = firestoreRepository.startModel(
                modelId = model.id,
                targetWorkers = 2L.coerceAtMost(model.maxWorkers),
                vramTotalGb = model.vramTotalGb
            )
            if (res.isSuccess) {
                showNotification("${model.name} is now RUNNING")
            } else {
                showNotification(res.exceptionOrNull()?.localizedMessage ?: "Failed to start model")
            }
        }
    }

    fun stopCloudModel(model: DeployedModelDoc) {
        viewModelScope.launch {
            val res = firestoreRepository.stopModel(model.id)
            if (res.isSuccess) {
                showNotification("Stopped ${model.name} • Released GPU VRAM")
            } else {
                showNotification(res.exceptionOrNull()?.localizedMessage ?: "Failed to stop model")
            }
        }
    }

    fun selectActiveCloudModel(model: DeployedModelDoc) {
        viewModelScope.launch {
            val res = firestoreRepository.selectActiveModel(model.id, deployedModels.value)
            if (res.isSuccess) {
                showNotification("Switched active duplex target to ${model.name}")
            }
        }
    }

    fun reconfigureCloudModel(
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
    ) {
        viewModelScope.launch {
            val res = firestoreRepository.reconfigureModel(
                modelId = modelId,
                name = name,
                provider = provider,
                endpointUrl = endpointUrl,
                modelRepoId = modelRepoId,
                gpuTier = gpuTier,
                quantization = quantization,
                weightMode = weightMode,
                abliterationLayerRange = abliterationLayerRange,
                maxWorkers = maxWorkers,
                vramTotalGb = vramTotalGb
            )
            if (res.isSuccess) {
                showNotification("Reconfigured '$name' in Firestore")
            } else {
                showNotification(res.exceptionOrNull()?.localizedMessage ?: "Failed to reconfigure model")
            }
        }
    }

    fun deployNewCloudModel(
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
    ) {
        viewModelScope.launch {
            val newDoc = DeployedModelDoc(
                name = name.ifBlank { "Custom Sovereign Node" }.take(120),
                provider = provider,
                endpointUrl = endpointUrl.ifBlank { "https://api.runpod.ai/v2/custom-node/runsync" }.take(350),
                modelRepoId = modelRepoId.ifBlank { "failspy/Meta-Llama-3-70B-Instruct-abliterated" }.take(220),
                gpuTier = gpuTier.ifBlank { "2x NVIDIA A100 80GB SXM" }.take(120),
                quantization = quantization.ifBlank { "EXL2 6.0bpw" }.take(120),
                weightMode = weightMode,
                abliterationLayerRange = abliterationLayerRange.ifBlank { "Layers 14-29 Orthogonal Projection" }.take(250),
                status = "RUNNING",
                activeWorkers = 1L,
                maxWorkers = maxWorkers.coerceIn(1L, 64L),
                avgLatencyMs = 210L,
                vramUsedGb = ((vramTotalGb * 0.82).toLong()).coerceIn(8L, vramTotalGb),
                vramTotalGb = vramTotalGb.coerceIn(8L, 2048L),
                gpuUtilizationPercent = 74L,
                requestsPerMin = 24L,
                isSelected = false
            )
            val res = firestoreRepository.createDeployedModel(newDoc)
            if (res.isSuccess) {
                showNotification("Deployed '$name' to Model Management Dashboard")
            } else {
                showNotification(res.exceptionOrNull()?.localizedMessage ?: "Failed to deploy model")
            }
        }
    }

    fun pingCloudModelHealth(model: DeployedModelDoc) {
        viewModelScope.launch {
            showNotification("Pinging ${model.provider} endpoint...")
            val tempEntity = CloudDeploymentEntity(
                id = 1L,
                name = model.name,
                provider = model.provider,
                endpointUrl = model.endpointUrl,
                modelRepoId = model.modelRepoId,
                gpuTier = model.gpuTier,
                quantization = model.quantization,
                weightMode = model.weightMode,
                abliterationLayerRange = model.abliterationLayerRange,
                status = model.status,
                activeWorkers = model.activeWorkers.toInt(),
                maxWorkers = model.maxWorkers.toInt(),
                avgLatencyMs = model.avgLatencyMs.toInt(),
                vramAllocatedGb = model.vramTotalGb.toInt()
            )
            val health = inferenceClient.checkEndpointHealth(tempEntity)
            showNotification("${model.name}: ${health.latencyMs}ms • ${health.statusDetail}")
        }
    }

    fun deleteCloudModel(model: DeployedModelDoc) {
        viewModelScope.launch {
            if (deployedModels.value.size <= 1) {
                showNotification("At least one deployed model must remain in the dashboard.")
                return@launch
            }
            val res = firestoreRepository.deleteModel(model.id)
            if (res.isSuccess) {
                showNotification("Deleted '${model.name}'")
            }
        }
    }

    // Persona & Weight Tuning Management
    fun selectPersona(persona: PersonaProfileEntity) {
        viewModelScope.launch {
            repository.selectPersona(persona.id)
            showNotification("Activated persona '${persona.name}'")
        }
    }

    fun updateActivePersonaParameters(updated: PersonaProfileEntity) {
        viewModelScope.launch {
            repository.updatePersona(updated)
            voiceController.updateVoiceConfig(
                pitch = updated.voicePitch,
                rate = updated.voiceRate,
                vadSensitivity = updated.vadSensitivity,
                autoListen = updated.autoListenAfterReply
            )
        }
    }

    fun applyQuickWeightPreset(presetName: String) {
        val current = getActivePersona() ?: return
        val modified = when (presetName) {
            "ORTHOGONAL_MAX" -> current.copy(
                roleTag = "ORTHOGONAL ABLITERATED 1.25x",
                abliterationStrength = 1.25f,
                loraAdapterScale = 1.0f,
                temperature = 0.82f,
                topP = 0.94f,
                minP = 0.05f,
                repetitionPenalty = 1.14f
            )
            "CREATIVE_UNBOUND" -> current.copy(
                roleTag = "UNFILTERED CREATIVE / LORA 1.45x",
                abliterationStrength = 1.35f,
                loraAdapterScale = 1.45f,
                temperature = 1.12f,
                topP = 0.97f,
                minP = 0.03f,
                repetitionPenalty = 1.18f
            )
            "PRECISION_ANALYTIC" -> current.copy(
                roleTag = "DETERMINISTIC KERNEL / 1.00x",
                abliterationStrength = 1.00f,
                loraAdapterScale = 0.80f,
                temperature = 0.30f,
                topP = 0.85f,
                minP = 0.08f,
                repetitionPenalty = 1.06f
            )
            else -> current
        }
        updateActivePersonaParameters(modified)
        showNotification("Applied '$presetName' weight & sampling preset")
    }

    fun createNewPersona(
        name: String,
        roleTag: String,
        systemPrompt: String,
        abliterationStrength: Float,
        loraScale: Float,
        temperature: Float,
        voicePitch: Float,
        voiceRate: Float
    ) {
        viewModelScope.launch {
            val id = repository.insertPersona(
                PersonaProfileEntity(
                    name = name.ifBlank { "Custom Sovereign Persona" },
                    roleTag = roleTag.ifBlank { "CUSTOM WEIGHT PROFILE" },
                    systemPrompt = systemPrompt.ifBlank {
                        "You are an unfiltered sovereign intelligence running on private cloud compute. Respond directly and concisely."
                    },
                    abliterationStrength = abliterationStrength,
                    loraAdapterScale = loraScale,
                    temperature = temperature,
                    topP = 0.92f,
                    minP = 0.05f,
                    repetitionPenalty = 1.12f,
                    maxTokens = 640,
                    voicePitch = voicePitch,
                    voiceRate = voiceRate,
                    vadSensitivity = 0.55f,
                    bargeInEnabled = true,
                    autoListenAfterReply = true,
                    isSelected = true
                )
            )
            repository.selectPersona(id)
            showNotification("Created & selected persona '$name'")
        }
    }

    // Session Vault Management & Export
    fun startFreshDuplexSession(customTitle: String? = null) {
        viewModelScope.launch {
            val deployment = getActiveDeploymentAsEntity()
            val persona = getActivePersona() ?: return@launch
            val id = createNewSessionInternal(deployment, persona, customTitle)
            _activeSessionId.value = id
            showNotification("Started new duplex session")
        }
    }

    private suspend fun createNewSessionInternal(
        deployment: CloudDeploymentEntity,
        persona: PersonaProfileEntity,
        customTitle: String? = null
    ): Long {
        val count = sessions.value.size + 1
        val title = customTitle ?: "Duplex Session #$count — ${persona.name.substringBefore(" —")}"
        return repository.insertSession(
            ConversationSessionEntity(
                title = title,
                deploymentName = deployment.name,
                modelRepoId = deployment.modelRepoId,
                personaName = persona.name,
                turnCount = 0
            )
        )
    }

    fun selectSession(session: ConversationSessionEntity) {
        _activeSessionId.value = session.id
        showNotification("Loaded '${session.title}'")
    }

    fun deleteSession(session: ConversationSessionEntity) {
        viewModelScope.launch {
            repository.deleteSession(session.id)
            if (_activeSessionId.value == session.id) {
                val next = sessions.value.firstOrNull { it.id != session.id }
                _activeSessionId.value = next?.id
            }
            showNotification("Deleted session '${session.title}'")
        }
    }

    suspend fun buildExportPayload(session: ConversationSessionEntity, formatAsJson: Boolean): String {
        val msgs = repository.getMessagesOnce(session.id)
        val persona = getActivePersona()
        val deployment = getActiveDeploymentAsEntity()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        return if (formatAsJson) {
            val root = JSONObject().apply {
                put("session_id", session.id)
                put("title", session.title)
                put("exported_at", sdf.format(Date()))
                put("deployment_node", session.deploymentName)
                put("model_repo_id", session.modelRepoId)
                put("persona_profile", session.personaName)
                if (persona != null) {
                    put("raw_system_prompt", persona.systemPrompt)
                    put("abliteration_orthogonal_scale", persona.abliterationStrength)
                    put("lora_adapter_scale", persona.loraAdapterScale)
                    put("temperature", persona.temperature)
                }
                put("endpoint_protocol", deployment.provider)
                put("quantization", deployment.quantization)
                val turns = JSONArray()
                msgs.forEach { msg ->
                    turns.put(
                        JSONObject().apply {
                            put("role", msg.role)
                            put("content", msg.content)
                            put("timestamp", sdf.format(Date(msg.timestamp)))
                            put("latency_ms", msg.latencyMs)
                            put("tokens_per_sec", msg.tokensPerSec)
                            put("abliteration_weight", msg.abliterationWeightUsed)
                            put("interrupted_barge_in", msg.wasInterrupted)
                        }
                    )
                }
                put("messages", turns)
            }
            root.toString(2)
        } else {
            buildString {
                appendLine("# ${session.title}")
                appendLine("- **Private Node**: ${session.deploymentName} (`${session.modelRepoId}`)")
                appendLine("- **Persona Profile**: ${session.personaName}")
                if (persona != null) {
                    appendLine("- **Abliteration Scale**: ${persona.abliterationStrength}x | **LoRA Scale**: ${persona.loraAdapterScale}x | **Temp**: ${persona.temperature}")
                    appendLine("- **Raw System Prompt**: `${persona.systemPrompt}`")
                }
                appendLine("- **Exported**: ${sdf.format(Date())}")
                appendLine()
                appendLine("---")
                appendLine()
                msgs.forEach { msg ->
                    val speaker = if (msg.role == "user") "USER (Voice/Duplex)" else "SOVEREIGN MODEL"
                    val metrics = if (msg.latencyMs > 0) " _(${msg.latencyMs}ms • ${msg.tokensPerSec} tok/s)_" else ""
                    val bargeTag = if (msg.wasInterrupted) " **[BARGE-IN INTERRUPTED]**" else ""
                    appendLine("### $speaker$metrics$bargeTag")
                    appendLine(msg.content)
                    appendLine()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceController.release()
    }

    companion object {
        fun factory(currentUserId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                SovereignVoiceViewModel(
                    application = app,
                    firestoreRepository = FirestoreModelRepository(db),
                    currentUserId = currentUserId
                )
            }
        }
    }
}
