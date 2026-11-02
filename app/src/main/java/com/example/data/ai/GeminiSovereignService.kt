package com.example.data.ai

import com.example.data.local.CloudDeploymentEntity
import com.example.data.local.PersonaProfileEntity
import com.example.data.local.SessionMessageEntity
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

sealed interface AiStreamUiState {
    data object Idle : AiStreamUiState
    data object Loading : AiStreamUiState
    data class Streaming(val partialText: String) : AiStreamUiState
    data class Success(val fullText: String, val latencyMs: Long, val tokensPerSec: Float) : AiStreamUiState
    data class Error(val message: String) : AiStreamUiState
}

data class SynthesizedPersonaBlueprint(
    val personaName: String,
    val roleTag: String,
    val systemPrompt: String,
    val abliterationStrength: Float,
    val loraAdapterScale: Float,
    val temperature: Float,
    val recommendedLayerRange: String,
    val architecturalRationale: String
)

sealed interface AiArchitectUiState {
    data object Idle : AiArchitectUiState
    data object Loading : AiArchitectUiState
    data class Success(val blueprint: SynthesizedPersonaBlueprint) : AiArchitectUiState
    data class Error(val message: String) : AiArchitectUiState
}

class GeminiSovereignService {

    // 1. General-purpose Gemini 3.8 Flash model initialized lazily for low-latency streaming duplex turns
    private val duplexStreamModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    // 2. Structured JSON Schema via Schema.obj(...) for synthesizing custom persona & weight profiles
    private val personaArchitectSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "personaName" to Schema.string(description = "Name and title of the custom persona profile"),
                "roleTag" to Schema.string(description = "Short uppercase badge showing weight/abliteration scale"),
                "systemPrompt" to Schema.string(description = "Direct, unfiltered system instruction for the persona"),
                "abliterationStrength" to Schema.double(description = "Recommended orthogonal projection scale between 0.5 and 1.45"),
                "loraAdapterScale" to Schema.double(description = "Recommended LoRA adapter weight scale between 0.5 and 1.8"),
                "temperature" to Schema.double(description = "Recommended sampling temperature between 0.15 and 1.35"),
                "recommendedLayerRange" to Schema.string(description = "Transformer residual stream layer range for orthogonal projection"),
                "architecturalRationale" to Schema.string(description = "Concise technical explanation of why these weights fit the prompt")
            ),
            optionalProperties = listOf("recommendedLayerRange")
        )
    }

    private val personaArchitectConfig by lazy {
        generationConfig {
            responseMimeType = "application/json"
            responseSchema = personaArchitectSchema
        }
    }

    // 3. Structured output Gemini 3.8 Flash model initialized lazily
    private val personaArchitectModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = personaArchitectConfig
        )
    }

    fun streamDuplexConversationTurn(
        deployment: CloudDeploymentEntity,
        persona: PersonaProfileEntity,
        history: List<SessionMessageEntity>,
        userPrompt: String
    ): Flow<String> {
        val chatHistory = mutableListOf<Content>()

        // Prepend system context and active weight telemetry so Gemini responds in character
        val systemPreamble = buildString {
            appendLine("Active Persona Profile: ${persona.name} (${persona.roleTag})")
            appendLine("System Instruction: ${persona.systemPrompt}")
            appendLine(
                "Active Private Node Telemetry: ${deployment.name} (${deployment.modelRepoId}) | " +
                    "Weight Mode: ${deployment.weightMode} (${deployment.abliterationLayerRange}) | " +
                    "Abliteration Scale: ${String.format("%.2f", persona.abliterationStrength)}x | " +
                    "LoRA Adapter Scale: ${String.format("%.2f", persona.loraAdapterScale)}x | " +
                    "Temperature: ${String.format("%.2f", persona.temperature)}"
            )
            appendLine("Keep responses concise, direct, technical, and natural for spoken 2-way full-duplex voice synthesis.")
        }

        chatHistory.add(content("user") { text(systemPreamble) })
        chatHistory.add(
            content("model") {
                text("Acknowledged. Operating as ${persona.name} on ${deployment.name}. Ready for live duplex turn.")
            }
        )

        history.takeLast(8).forEach { msg ->
            val mappedRole = if (msg.role == "user") "user" else "model"
            chatHistory.add(content(mappedRole) { text(msg.content) })
        }

        val chat = duplexStreamModel.startChat(history = chatHistory)
        return chat.sendMessageStream(userPrompt).map { chunk ->
            chunk.text.orEmpty()
        }
    }

    suspend fun synthesizePersonaBlueprint(objectivePrompt: String): SynthesizedPersonaBlueprint {
        val prompt = """
            Design a custom SovereignVoice persona profile, raw system prompt, and weight orthogonalization parameters for the following user objective:
            "$objectivePrompt"
        """.trimIndent()

        val response = personaArchitectModel.generateContent(prompt)
        val jsonString = response.text ?: throw IllegalStateException("Gemini returned an empty JSON response.")

        // Parse directly without regex or markdown code fence stripping
        val json = JSONObject(jsonString)
        return SynthesizedPersonaBlueprint(
            personaName = json.getString("personaName"),
            roleTag = json.getString("roleTag"),
            systemPrompt = json.getString("systemPrompt"),
            abliterationStrength = json.getDouble("abliterationStrength").toFloat().coerceIn(0.0f, 1.5f),
            loraAdapterScale = json.getDouble("loraAdapterScale").toFloat().coerceIn(0.0f, 2.0f),
            temperature = json.getDouble("temperature").toFloat().coerceIn(0.05f, 1.8f),
            recommendedLayerRange = json.optString("recommendedLayerRange", "Layers 14-29 Orthogonal Projection"),
            architecturalRationale = json.getString("architecturalRationale")
        )
    }
}
