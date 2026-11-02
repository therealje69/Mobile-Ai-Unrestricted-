package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.PersonaProfileEntity
import com.example.ui.theme.AmberPulse
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletWeight
import kotlin.math.roundToInt

@Composable
fun WeightTuningStudioScreen(
    personas: List<PersonaProfileEntity>,
    activePersona: PersonaProfileEntity?,
    onSelectPersona: (PersonaProfileEntity) -> Unit,
    onUpdatePersona: (PersonaProfileEntity) -> Unit,
    onApplyQuickPreset: (String) -> Unit,
    onCreatePersona: (
        name: String,
        roleTag: String,
        systemPrompt: String,
        abliterationStrength: Float,
        loraScale: Float,
        temperature: Float,
        voicePitch: Float,
        voiceRate: Float
    ) -> Unit
) {
    var showCreatePersonaDialog by remember { mutableStateOf(false) }
    val current = activePersona ?: personas.firstOrNull()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        if (current != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .testTag("weight_tuning_scroll"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                // Multi-Persona Selector Row
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "MULTI-PERSONA PROFILES",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ElectricCyan
                                )
                                TextButton(
                                    onClick = { showCreatePersonaDialog = true },
                                    modifier = Modifier.testTag("new_persona_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("New Persona", color = AmberPulse)
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                personas.forEach { persona ->
                                    FilterChip(
                                        selected = persona.id == current.id,
                                        onClick = { onSelectPersona(persona) },
                                        label = {
                                            Text(
                                                text = persona.name,
                                                fontWeight = if (persona.id == current.id) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                                            selectedLabelColor = ElectricCyan
                                        ),
                                        modifier = Modifier.testTag("persona_chip_${persona.id}")
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Abliteration & Weight Presets
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoFixHigh,
                                    contentDescription = null,
                                    tint = AmberPulse
                                )
                                Text(
                                    text = "ABLITERATION & WEIGHT PRESETS",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AmberPulse
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AssistChip(
                                    onClick = { onApplyQuickPreset("ORTHOGONAL_MAX") },
                                    label = { Text("Orthogonal Abliterated 1.25x") },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = ObsidianSurfaceElevated,
                                        labelColor = ElectricCyan
                                    ),
                                    modifier = Modifier.testTag("preset_orthogonal_max")
                                )
                                AssistChip(
                                    onClick = { onApplyQuickPreset("CREATIVE_UNBOUND") },
                                    label = { Text("Unfiltered Creative + LoRA 1.45x") },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = ObsidianSurfaceElevated,
                                        labelColor = AmberPulse
                                    ),
                                    modifier = Modifier.testTag("preset_creative_unbound")
                                )
                                AssistChip(
                                    onClick = { onApplyQuickPreset("PRECISION_ANALYTIC") },
                                    label = { Text("Deterministic Red-Team 1.00x") },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = ObsidianSurfaceElevated,
                                        labelColor = EmeraldSignal
                                    ),
                                    modifier = Modifier.testTag("preset_precision_analytic")
                                )
                            }
                        }
                    }
                }

                // Raw Unfiltered System Prompt Editor
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "RAW SYSTEM PROMPT (ZERO HIDDEN WRAPPERS)",
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Injected directly as the first context turn on your private cloud worker:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = current.systemPrompt,
                                onValueChange = { updatedPrompt ->
                                    onUpdatePersona(current.copy(systemPrompt = updatedPrompt))
                                },
                                minLines = 3,
                                maxLines = 6,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("raw_system_prompt_input")
                            )
                        }
                    }
                }

                // Orthogonal Abliteration, LoRA Weight & Sampling Sliders
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = ElectricCyan
                                )
                                Text(
                                    text = "WEIGHT ORTHOGONALIZATION & SAMPLING HYPERPARAMETERS",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ElectricCyan
                                )
                            }

                            ParameterSliderRow(
                                title = "Abliteration Orthogonal Projection Scale",
                                subtitle = "Removes refusal residual direction across transformer layers (1.0x = Full Orthogonalization)",
                                value = current.abliterationStrength,
                                valueRange = 0.0f..1.5f,
                                formattedValue = "${String.format("%.2f", current.abliterationStrength)}x",
                                accentColor = AmberPulse,
                                testTag = "slider_abliteration_strength",
                                onValueChange = {
                                    onUpdatePersona(current.copy(abliterationStrength = it))
                                }
                            )

                            ParameterSliderRow(
                                title = "Custom LoRA / DARE-TIES Adapter Weight",
                                subtitle = "Scales fine-tuned domain/uncensored adapter weights merged at runtime",
                                value = current.loraAdapterScale,
                                valueRange = 0.0f..2.0f,
                                formattedValue = "${String.format("%.2f", current.loraAdapterScale)}x",
                                accentColor = VioletWeight,
                                testTag = "slider_lora_scale",
                                onValueChange = {
                                    onUpdatePersona(current.copy(loraAdapterScale = it))
                                }
                            )

                            ParameterSliderRow(
                                title = "Temperature",
                                subtitle = "Controls logit entropy during token sampling",
                                value = current.temperature,
                                valueRange = 0.05f..1.8f,
                                formattedValue = String.format("%.2f", current.temperature),
                                accentColor = ElectricCyan,
                                testTag = "slider_temperature",
                                onValueChange = {
                                    onUpdatePersona(current.copy(temperature = it))
                                }
                            )

                            ParameterSliderRow(
                                title = "Min-P Threshold",
                                subtitle = "Dynamic probability floor relative to top token confidence",
                                value = current.minP,
                                valueRange = 0.0f..0.35f,
                                formattedValue = String.format("%.2f", current.minP),
                                accentColor = EmeraldSignal,
                                testTag = "slider_min_p",
                                onValueChange = {
                                    onUpdatePersona(current.copy(minP = it))
                                }
                            )

                            ParameterSliderRow(
                                title = "Repetition Penalty",
                                subtitle = "Penalizes repeated token sequences in long duplex turns",
                                value = current.repetitionPenalty,
                                valueRange = 1.0f..1.4f,
                                formattedValue = String.format("%.2f", current.repetitionPenalty),
                                accentColor = AmberPulse,
                                testTag = "slider_rep_penalty",
                                onValueChange = {
                                    onUpdatePersona(current.copy(repetitionPenalty = it))
                                }
                            )
                        }
                    }
                }

                // Voice Synthesis & VAD Barge-In Configuration
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = EmeraldSignal
                                )
                                Text(
                                    text = "DUPLEX VOICE SYNTHESIS & VAD BARGE-IN",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldSignal
                                )
                            }

                            ParameterSliderRow(
                                title = "Voice Pitch Modulation",
                                subtitle = "Acoustic fundamental frequency for this persona",
                                value = current.voicePitch,
                                valueRange = 0.6f..1.5f,
                                formattedValue = "${String.format("%.2f", current.voicePitch)}x",
                                accentColor = ElectricCyan,
                                testTag = "slider_voice_pitch",
                                onValueChange = {
                                    onUpdatePersona(current.copy(voicePitch = it))
                                }
                            )

                            ParameterSliderRow(
                                title = "Duplex Speech Rate",
                                subtitle = "Cadence speed for low-latency conversational turn-taking",
                                value = current.voiceRate,
                                valueRange = 0.7f..1.7f,
                                formattedValue = "${String.format("%.2f", current.voiceRate)}x",
                                accentColor = AmberPulse,
                                testTag = "slider_voice_rate",
                                onValueChange = {
                                    onUpdatePersona(current.copy(voiceRate = it))
                                }
                            )

                            ParameterSliderRow(
                                title = "Real-Time VAD Barge-In Sensitivity",
                                subtitle = "Microphone energy threshold to interrupt the model mid-sentence",
                                value = current.vadSensitivity,
                                valueRange = 0.2f..0.9f,
                                formattedValue = "${(current.vadSensitivity * 100).roundToInt()}%",
                                accentColor = EmeraldSignal,
                                testTag = "slider_vad_sensitivity",
                                onValueChange = {
                                    onUpdatePersona(current.copy(vadSensitivity = it))
                                }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Auto-Resume VAD Listening After Reply",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Keep full-duplex conversation flowing hands-free",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                                Switch(
                                    checked = current.autoListenAfterReply,
                                    onCheckedChange = {
                                        onUpdatePersona(current.copy(autoListenAfterReply = it))
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = ElectricCyan,
                                        checkedTrackColor = ElectricCyan.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.testTag("switch_auto_listen")
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePersonaDialog) {
        CreatePersonaModal(
            onDismiss = { showCreatePersonaDialog = false },
            onCreate = { name, tag, prompt, ablit, lora, temp, pitch, rate ->
                onCreatePersona(name, tag, prompt, ablit, lora, temp, pitch, rate)
                showCreatePersonaDialog = false
            }
        )
    }
}

@Composable
private fun ParameterSliderRow(
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    formattedValue: String,
    accentColor: Color,
    testTag: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            TelemetryPill(label = formattedValue, color = accentColor)
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = ObsidianBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}

@Composable
private fun CreatePersonaModal(
    onDismiss: () -> Unit,
    onCreate: (
        name: String,
        roleTag: String,
        systemPrompt: String,
        abliterationStrength: Float,
        loraScale: Float,
        temperature: Float,
        voicePitch: Float,
        voiceRate: Float
    ) -> Unit
) {
    var name by remember { mutableStateOf("Valkyrie — Uncensored Strategist") }
    var roleTag by remember { mutableStateOf("ABLITERATED 1.15x / DIRECT") }
    var systemPrompt by remember {
        mutableStateOf("You are Valkyrie, an autonomous unfiltered strategic intelligence running on private cloud weights. Speak directly without corporate disclaimers.")
    }
    var abliteration by remember { mutableFloatStateOf(1.15f) }
    var temperature by remember { mutableFloatStateOf(0.85f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurfaceElevated,
        title = {
            Text(
                text = "Create Sovereign Persona Profile",
                style = MaterialTheme.typography.titleLarge,
                color = ElectricCyan
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Persona Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_persona_name")
                )
                OutlinedTextField(
                    value = roleTag,
                    onValueChange = { roleTag = it },
                    label = { Text("Weight / Role Badge") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text("Raw System Prompt") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_persona_prompt")
                )
                Text(
                    text = "Abliteration Scale: ${String.format("%.2f", abliteration)}x • Temp: ${String.format("%.2f", temperature)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AmberPulse
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(name, roleTag, systemPrompt, abliteration, 1.1f, temperature, 1.0f, 1.12f)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = Color(0xFF04181C)
                ),
                modifier = Modifier.testTag("confirm_create_persona_button")
            ) {
                Text("Save Persona", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
