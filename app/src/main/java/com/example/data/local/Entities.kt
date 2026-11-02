package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cloud_deployments")
data class CloudDeploymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val provider: String, // "RUNPOD_SERVERLESS" or "HUGGINGFACE_ENDPOINT"
    val endpointUrl: String,
    val modelRepoId: String,
    val gpuTier: String,
    val quantization: String,
    val weightMode: String, // "ORTHOGONAL_ABLITERATED", "CUSTOM_LORA_WEIGHTED", "RAW_FOUNDATION_MERGE"
    val abliterationLayerRange: String, // e.g. "Layers 12-28 Refusal Direction Removed"
    val status: String, // "ONLINE", "WARMING_UP", "SCALING", "STANDBY"
    val activeWorkers: Int,
    val maxWorkers: Int,
    val avgLatencyMs: Int,
    val vramAllocatedGb: Int,
    val customAuthHeader: String = "",
    val isSelected: Boolean = false
)

@Entity(tableName = "persona_profiles")
data class PersonaProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val roleTag: String,
    val systemPrompt: String,
    val abliterationStrength: Float, // 0.0f .. 1.5f (orthogonal direction scale)
    val loraAdapterScale: Float,     // 0.0f .. 2.0f
    val temperature: Float,          // 0.0f .. 2.0f
    val topP: Float,                 // 0.05f .. 1.0f
    val minP: Float,                 // 0.0f .. 0.5f
    val repetitionPenalty: Float,    // 1.0f .. 1.5f
    val maxTokens: Int,              // 128 .. 4096
    val voicePitch: Float,           // 0.5f .. 1.6f
    val voiceRate: Float,            // 0.6f .. 1.8f
    val vadSensitivity: Float,       // 0.1f .. 0.9f (Barge-In threshold)
    val bargeInEnabled: Boolean = true,
    val autoListenAfterReply: Boolean = true,
    val isSelected: Boolean = false
)

@Entity(tableName = "conversation_sessions")
data class ConversationSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val deploymentName: String,
    val modelRepoId: String,
    val personaName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val turnCount: Int = 0
)

@Entity(tableName = "session_messages")
data class SessionMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val role: String, // "user", "assistant", "system_telemetry"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val latencyMs: Long = 0L,
    val tokensPerSec: Float = 0f,
    val abliterationWeightUsed: Float = 1.0f,
    val wasInterrupted: Boolean = false,
    val endpointProtocol: String = "RUNPOD_SERVERLESS"
)
