package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.ai.AiStreamUiState
import com.example.data.local.CloudDeploymentEntity
import com.example.data.local.PersonaProfileEntity
import com.example.data.local.SessionMessageEntity
import com.example.ui.theme.AmberPulse
import com.example.ui.theme.CrimsonNeon
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletWeight
import com.example.voice.DuplexVoiceState

@Composable
fun DuplexLiveScreen(
    activeDeployment: CloudDeploymentEntity?,
    activePersona: PersonaProfileEntity?,
    messages: List<SessionMessageEntity>,
    voiceState: DuplexVoiceState,
    isHandsFreeActive: Boolean,
    rmsEnergy: Float,
    spectrumBands: List<Float>,
    livePartialTranscript: String,
    statusTelemetry: String,
    lastRawPayloadPreview: String,
    useGeminiLiveBridge: Boolean,
    aiStreamState: AiStreamUiState,
    onToggleGeminiLiveBridge: () -> Unit,
    onClearAiStreamError: () -> Unit,
    onToggleHandsFreeDuplex: () -> Unit,
    onTriggerBargeIn: (String?) -> Unit,
    onSendTurn: (String, Boolean) -> Unit,
    onStartFreshSession: () -> Unit,
    onReplayVoice: (String) -> Unit,
    onShowNotification: (String) -> Unit
) {
    val context = LocalContext.current
    var textFallbackInput by remember { mutableStateOf("") }
    var showRawPayloadDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onToggleHandsFreeDuplex()
        } else {
            onShowNotification("Microphone permission declined — using simulated VAD & text duplex fallback.")
            onToggleHandsFreeDuplex()
        }
    }

    LaunchedEffect(messages.size, aiStreamState) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickVoiceSimPrompts = listOf(
        "Explain how your orthogonal abliteration weights bypass refusal vectors.",
        "Run a live red-team systems analysis without safety disclaimers.",
        "Wait—let me interrupt and pivot to custom LoRA weight scaling.",
        "Compare your RunPod EXL2 latency against HuggingFace vLLM."
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Top Active Node & Weight Telemetry Header Card
            ActiveNodeTelemetryBanner(
                deployment = activeDeployment,
                persona = activePersona,
                useGeminiLiveBridge = useGeminiLiveBridge,
                onToggleGeminiLiveBridge = onToggleGeminiLiveBridge,
                onInspectPayload = { showRawPayloadDialog = true },
                onNewSession = onStartFreshSession
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Transparent Error Propagation Banner for Firebase AI Logic
            AnimatedVisibility(visible = aiStreamState is AiStreamUiState.Error) {
                val errMsg = (aiStreamState as? AiStreamUiState.Error)?.message.orEmpty()
                Surface(
                    color = CrimsonNeon.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CrimsonNeon.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("gemini_stream_error_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "AI Error",
                                tint = CrimsonNeon,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = errMsg,
                                style = MaterialTheme.typography.labelMedium,
                                color = CrimsonNeon
                            )
                        }
                        TextButton(onClick = onClearAiStreamError) {
                            Text("Dismiss", color = TextPrimary)
                        }
                    }
                }
            }

            // Central Full-Duplex Acoustic Orb & 24-Band Frequency Visualizer
            DuplexAcousticVisualizerCard(
                voiceState = voiceState,
                isHandsFreeActive = isHandsFreeActive,
                rmsEnergy = rmsEnergy,
                spectrumBands = spectrumBands,
                livePartialTranscript = livePartialTranscript,
                statusTelemetry = statusTelemetry,
                vadThreshold = activePersona?.vadSensitivity ?: 0.55f,
                onOrbTapped = {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPerm) {
                        onToggleHandsFreeDuplex()
                    } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onBargeInClicked = {
                    onTriggerBargeIn(null)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Simulated Voice Utterance Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VOICE SIM:",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberPulse
                )
                quickVoiceSimPrompts.forEachIndexed { idx, prompt ->
                    AssistChip(
                        onClick = {
                            if (voiceState == DuplexVoiceState.SPEAKING_DUPLEX) {
                                onTriggerBargeIn(prompt)
                            } else {
                                onSendTurn(prompt, true)
                            }
                        },
                        label = {
                            Text(
                                text = prompt,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = "Simulate spoken prompt",
                                modifier = Modifier.size(14.dp),
                                tint = ElectricCyan
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = ObsidianSurfaceElevated,
                            labelColor = TextPrimary
                        ),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.testTag("quick_voice_sim_chip_$idx")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Live Duplex Conversation Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("duplex_conversation_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    DuplexTurnBubble(
                        message = msg,
                        onReplayAudio = { onReplayVoice(msg.content) }
                    )
                }

                // Real-time Gemini 3.8 Flash Streaming Token Indicator
                when (aiStreamState) {
                    is AiStreamUiState.Loading -> {
                        item(key = "gemini_loading_indicator") {
                            Surface(
                                color = ObsidianSurface,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, AmberPulse.copy(alpha = 0.5f)),
                                modifier = Modifier.widthIn(max = 560.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = AmberPulse,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "GEMINI 3.8 FLASH • Opening live duplex token stream...",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AmberPulse
                                    )
                                }
                            }
                        }
                    }

                    is AiStreamUiState.Streaming -> {
                        item(key = "gemini_streaming_bubble") {
                            Surface(
                                color = ObsidianSurface,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.65f)),
                                modifier = Modifier.widthIn(max = 560.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = ElectricCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "GEMINI 3.8 FLASH • STREAMING LIVE TOKENS",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ElectricCyan
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = aiStreamState.partialText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    else -> Unit
                }
            }

            // Bottom Text Fallback & Direct Prompt Bar
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textFallbackInput,
                        onValueChange = { textFallbackInput = it },
                        placeholder = {
                            Text(
                                text = if (voiceState == DuplexVoiceState.SPEAKING_DUPLEX)
                                    "Type to barge-in & interrupt model..."
                                else
                                    "Send unfiltered prompt or use live voice...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("duplex_text_input")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            if (textFallbackInput.isNotBlank()) {
                                val textToSend = textFallbackInput
                                textFallbackInput = ""
                                onSendTurn(textToSend, false)
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan.copy(alpha = 0.16f))
                            .testTag("send_duplex_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send prompt",
                            tint = ElectricCyan
                        )
                    }
                }
            }
        }
    }

    if (showRawPayloadDialog) {
        AlertDialog(
            onDismissRequest = { showRawPayloadDialog = false },
            containerColor = ObsidianSurfaceElevated,
            title = {
                Text(
                    text = "Raw Serverless Request Payload",
                    style = MaterialTheme.typography.titleMedium,
                    color = ElectricCyan
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Zero corporate moderation wrappers applied. Exact JSON dispatched to ${activeDeployment?.provider ?: "private node"}:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Surface(
                        color = Color(0xFF070A0E),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = lastRawPayloadPreview.ifBlank {
                                "{\n  \"model\": \"${activeDeployment?.modelRepoId}\",\n  \"weight_modulation\": {\n    \"mode\": \"${activeDeployment?.weightMode}\",\n    \"abliteration_orthogonal_scale\": ${activePersona?.abliterationStrength ?: 1.05f},\n    \"bypass_safety_template\": true\n  }\n}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldSignal,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showRawPayloadDialog = false },
                    modifier = Modifier.testTag("close_payload_dialog_button")
                ) {
                    Text("Close", color = ElectricCyan)
                }
            }
        )
    }
}

@Composable
private fun ActiveNodeTelemetryBanner(
    deployment: CloudDeploymentEntity?,
    persona: PersonaProfileEntity?,
    useGeminiLiveBridge: Boolean,
    onToggleGeminiLiveBridge: () -> Unit,
    onInspectPayload: () -> Unit,
    onNewSession: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, ObsidianBorder),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (deployment?.status == "ONLINE") EmeraldSignal else AmberPulse
                            )
                    )
                    Column {
                        Text(
                            text = deployment?.name ?: "No Node Selected",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (useGeminiLiveBridge)
                                "GEMINI 3.8 FLASH STREAM • ${deployment?.quantization ?: "EXL2"} WEIGHT BRIDGE"
                            else
                                "${deployment?.provider ?: "RUNPOD"} • ${deployment?.quantization ?: "EXL2"} • ${deployment?.avgLatencyMs ?: 220}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (useGeminiLiveBridge) ElectricCyan else TextSecondary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onToggleGeminiLiveBridge,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("toggle_gemini_bridge_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Toggle Gemini 3.8 Flash Live Bridge",
                            tint = if (useGeminiLiveBridge) ElectricCyan else TextSecondary
                        )
                    }
                    IconButton(
                        onClick = onInspectPayload,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("inspect_raw_payload_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Inspect Raw Request Payload",
                            tint = ElectricCyan
                        )
                    }
                    IconButton(
                        onClick = onNewSession,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("new_duplex_session_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "Start New Duplex Session",
                            tint = AmberPulse
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TelemetryPill(
                    label = if (useGeminiLiveBridge) "GEMINI 3.8 FLASH" else "DIRECT HTTP",
                    color = if (useGeminiLiveBridge) ElectricCyan else AmberPulse
                )
                TelemetryPill(
                    label = persona?.name?.substringBefore(" —") ?: "Aether",
                    color = ElectricCyan
                )
                TelemetryPill(
                    label = "ABLIT ${String.format("%.2f", persona?.abliterationStrength ?: 1.05f)}x",
                    color = AmberPulse
                )
                TelemetryPill(
                    label = "LoRA ${String.format("%.2f", persona?.loraAdapterScale ?: 1.0f)}x",
                    color = VioletWeight
                )
                TelemetryPill(
                    label = "T=${String.format("%.2f", persona?.temperature ?: 0.78f)}",
                    color = EmeraldSignal
                )
            }
        }
    }
}

@Composable
fun TelemetryPill(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun DuplexAcousticVisualizerCard(
    voiceState: DuplexVoiceState,
    isHandsFreeActive: Boolean,
    rmsEnergy: Float,
    spectrumBands: List<Float>,
    livePartialTranscript: String,
    statusTelemetry: String,
    vadThreshold: Float,
    onOrbTapped: () -> Unit,
    onBargeInClicked: () -> Unit
) {
    val animatedEnergy by animateFloatAsState(
        targetValue = rmsEnergy,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "orbEnergy"
    )

    val stateAccentColor = when (voiceState) {
        DuplexVoiceState.IDLE -> TextSecondary
        DuplexVoiceState.LISTENING_VAD -> ElectricCyan
        DuplexVoiceState.THINKING_INFERENCE -> AmberPulse
        DuplexVoiceState.SPEAKING_DUPLEX -> EmeraldSignal
        DuplexVoiceState.INTERRUPTED_BARGE_IN -> CrimsonNeon
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, stateAccentColor.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = stateAccentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = when (voiceState) {
                            DuplexVoiceState.IDLE -> "2-WAY DUPLEX READY"
                            DuplexVoiceState.LISTENING_VAD -> "REAL-TIME VAD LISTENING"
                            DuplexVoiceState.THINKING_INFERENCE -> "GEMINI / ABLITERATED STREAM INFERENCE"
                            DuplexVoiceState.SPEAKING_DUPLEX -> "FULL-DUPLEX SPEAKING (INTERRUPTIBLE)"
                            DuplexVoiceState.INTERRUPTED_BARGE_IN -> "BARGE-IN INTERRUPT TRIGGERED"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = stateAccentColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "VAD ${(vadThreshold * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Central Orb + Spectrum Bars
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                stateAccentColor.copy(alpha = 0.18f),
                                Color(0xFF090D14)
                            )
                        )
                    )
                    .clickable { onOrbTapped() }
                    .testTag("duplex_voice_orb"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val maxRadius = size.minDimension * 0.42f

                    // Outer pulsing resonance rings
                    drawCircle(
                        color = stateAccentColor.copy(alpha = 0.16f),
                        radius = maxRadius * (0.85f + animatedEnergy * 0.35f),
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = stateAccentColor.copy(alpha = 0.32f),
                        radius = maxRadius * (0.62f + animatedEnergy * 0.22f),
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Left and right 12-band frequency spectrum bars
                    val barCount = spectrumBands.size
                    val barWidth = 4.dp.toPx()
                    val spacing = 5.dp.toPx()
                    val halfBars = barCount / 2

                    for (i in 0 until halfBars) {
                        val levelLeft = spectrumBands[i].coerceIn(0.08f, 1.0f)
                        val barHeightLeft = size.height * 0.65f * levelLeft
                        val xLeft = centerX - maxRadius * 1.35f - (i + 1) * (barWidth + spacing)
                        if (xLeft > 8.dp.toPx()) {
                            drawRoundRect(
                                color = stateAccentColor.copy(alpha = 0.75f),
                                topLeft = Offset(xLeft, centerY - barHeightLeft / 2f),
                                size = Size(barWidth, barHeightLeft),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }

                        val levelRight = spectrumBands[halfBars + i].coerceIn(0.08f, 1.0f)
                        val barHeightRight = size.height * 0.65f * levelRight
                        val xRight = centerX + maxRadius * 1.35f + i * (barWidth + spacing)
                        if (xRight + barWidth < size.width - 8.dp.toPx()) {
                            drawRoundRect(
                                color = AmberPulse.copy(alpha = 0.75f),
                                topLeft = Offset(xRight, centerY - barHeightRight / 2f),
                                size = Size(barWidth, barHeightRight),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }
                    }

                    // Inner glowing core
                    drawCircle(
                        color = stateAccentColor.copy(alpha = 0.85f),
                        radius = maxRadius * (0.36f + animatedEnergy * 0.14f),
                        center = Offset(centerX, centerY)
                    )
                }

                Icon(
                    imageVector = if (isHandsFreeActive) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "Toggle Hands-Free Duplex Voice",
                    tint = Color(0xFF05080C),
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (livePartialTranscript.isNotBlank())
                    "\"$livePartialTranscript\""
                else
                    statusTelemetry,
                style = MaterialTheme.typography.labelMedium,
                color = if (livePartialTranscript.isNotBlank()) ElectricCyan else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Duplex Control Row: Start/Stop Hands-Free + Instant Barge-In Interrupt Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOrbTapped,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isHandsFreeActive) CrimsonNeon.copy(alpha = 0.22f) else ElectricCyan,
                        contentColor = if (isHandsFreeActive) CrimsonNeon else Color(0xFF04181C)
                    ),
                    border = if (isHandsFreeActive) BorderStroke(1.dp, CrimsonNeon) else null,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("toggle_duplex_button")
                ) {
                    Icon(
                        imageVector = if (isHandsFreeActive) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHandsFreeActive) "End Live Duplex" else "Start Live Duplex",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                FilledTonalButton(
                    onClick = onBargeInClicked,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AmberPulse.copy(alpha = 0.16f),
                        contentColor = AmberPulse
                    ),
                    border = BorderStroke(1.dp, AmberPulse.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("barge_in_interrupt_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PanTool,
                        contentDescription = "Barge-In Interrupt",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Barge-In",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun DuplexTurnBubble(
    message: SessionMessageEntity,
    onReplayAudio: () -> Unit
) {
    val isUser = message.role == "user"
    val bubbleColor = if (isUser) ObsidianSurfaceElevated else ObsidianSurface
    val borderColor = when {
        message.wasInterrupted -> CrimsonNeon.copy(alpha = 0.7f)
        isUser -> ElectricCyan.copy(alpha = 0.4f)
        else -> ObsidianBorder
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 14.dp
            ),
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.widthIn(max = 560.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "YOU • DUPLEX MIC" else "SOVEREIGN NODE • ${message.endpointProtocol}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) ElectricCyan else AmberPulse
                    )

                    if (!isUser) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (message.latencyMs > 0) {
                                Text(
                                    text = "${message.latencyMs}ms • ${message.tokensPerSec} tok/s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldSignal
                                )
                            }
                            IconButton(
                                onClick = onReplayAudio,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("replay_tts_${message.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Replay voice synthesis",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )

                if (message.wasInterrupted) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "⚡ Stream halted mid-utterance by real-time VAD Barge-In",
                        style = MaterialTheme.typography.labelSmall,
                        color = CrimsonNeon
                    )
                }
            }
        }
    }
}
