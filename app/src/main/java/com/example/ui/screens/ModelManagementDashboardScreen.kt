package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.firestore.DeployedModelDoc
import com.example.ui.theme.AmberPulse
import com.example.ui.theme.CrimsonNeon
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletWeight

@Composable
fun ModelManagementDashboardScreen(
    models: List<DeployedModelDoc>,
    onStartModel: (DeployedModelDoc) -> Unit,
    onStopModel: (DeployedModelDoc) -> Unit,
    onSelectActiveModel: (DeployedModelDoc) -> Unit,
    onPingModel: (DeployedModelDoc) -> Unit,
    onDeleteModel: (DeployedModelDoc) -> Unit,
    onReconfigureModel: (
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
    ) -> Unit,
    onDeployNewModel: (
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
    ) -> Unit
) {
    var statusFilter by remember { mutableStateOf("ALL") }
    var showDeployModal by remember { mutableStateOf(false) }
    var modelToReconfigure by remember { mutableStateOf<DeployedModelDoc?>(null) }

    val filteredModels = remember(models, statusFilter) {
        when (statusFilter) {
            "RUNNING" -> models.filter { it.status == "RUNNING" || it.status == "WARMING_UP" }
            "STOPPED" -> models.filter { it.status == "STOPPED" }
            else -> models
        }
    }

    val runningCount = models.count { it.status == "RUNNING" }
    val stoppedCount = models.count { it.status == "STOPPED" }
    val totalVramUsed = models.sumOf { it.vramUsedGb }
    val totalVramCapacity = models.sumOf { it.vramTotalGb }.coerceAtLeast(1L)
    val avgGpuUtilization = if (runningCount > 0) {
        models.filter { it.status == "RUNNING" }.map { it.gpuUtilizationPercent }.average().toInt()
    } else 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("model_management_dashboard_list"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 88.dp)
        ) {
            // Fleet Overview & Aggregate Resource Utilization Summary Card
            item {
                FleetTelemetrySummaryCard(
                    totalModels = models.size,
                    runningCount = runningCount,
                    stoppedCount = stoppedCount,
                    totalVramUsed = totalVramUsed,
                    totalVramCapacity = totalVramCapacity,
                    avgGpuUtilization = avgGpuUtilization,
                    onOpenDeployDialog = { showDeployModal = true }
                )
            }

            // Status Filter Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = statusFilter == "ALL",
                        onClick = { statusFilter = "ALL" },
                        label = { Text("All Models (${models.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                            selectedLabelColor = ElectricCyan
                        ),
                        modifier = Modifier.testTag("filter_models_all")
                    )
                    FilterChip(
                        selected = statusFilter == "RUNNING",
                        onClick = { statusFilter = "RUNNING" },
                        label = { Text("Running ($runningCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldSignal.copy(alpha = 0.2f),
                            selectedLabelColor = EmeraldSignal
                        ),
                        modifier = Modifier.testTag("filter_models_running")
                    )
                    FilterChip(
                        selected = statusFilter == "STOPPED",
                        onClick = { statusFilter = "STOPPED" },
                        label = { Text("Stopped ($stoppedCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CrimsonNeon.copy(alpha = 0.2f),
                            selectedLabelColor = CrimsonNeon
                        ),
                        modifier = Modifier.testTag("filter_models_stopped")
                    )
                }
            }

            items(filteredModels, key = { it.id.ifBlank { it.name } }) { model ->
                DeployedModelDashboardCard(
                    model = model,
                    onStart = { onStartModel(model) },
                    onStop = { onStopModel(model) },
                    onReconfigure = { modelToReconfigure = model },
                    onSelectActive = { onSelectActiveModel(model) },
                    onPing = { onPingModel(model) },
                    onDelete = { onDeleteModel(model) }
                )
            }
        }

        FloatingActionButton(
            onClick = { showDeployModal = true },
            containerColor = ElectricCyan,
            contentColor = Color(0xFF04181C),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("fab_deploy_new_model")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Deploy New Model")
                Text(
                    text = "Deploy Model",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showDeployModal) {
        ModelConfigurationModal(
            title = "Deploy New Cloud Model Instance",
            initialModel = null,
            onDismiss = { showDeployModal = false },
            onConfirm = { name, provider, url, repo, gpu, quant, weightMode, layers, maxW, vram ->
                onDeployNewModel(name, provider, url, repo, gpu, quant, weightMode, layers, maxW, vram)
                showDeployModal = false
            }
        )
    }

    val editing = modelToReconfigure
    if (editing != null) {
        ModelConfigurationModal(
            title = "Reconfigure Deployed Model",
            initialModel = editing,
            onDismiss = { modelToReconfigure = null },
            onConfirm = { name, provider, url, repo, gpu, quant, weightMode, layers, maxW, vram ->
                onReconfigureModel(
                    editing.id, name, provider, url, repo, gpu, quant, weightMode, layers, maxW, vram
                )
                modelToReconfigure = null
            }
        )
    }
}

@Composable
private fun FleetTelemetrySummaryCard(
    totalModels: Int,
    runningCount: Int,
    stoppedCount: Int,
    totalVramUsed: Long,
    totalVramCapacity: Long,
    avgGpuUtilization: Int,
    onOpenDeployDialog: () -> Unit
) {
    val vramRatio = (totalVramUsed.toFloat() / totalVramCapacity.toFloat()).coerceIn(0f, 1f)

    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, ObsidianBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_cloud_node_hero),
                    contentDescription = "Model Management Dashboard Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "FIRESTORE CLOUD-SYNCED FLEET TELEMETRY",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                    Text(
                        text = "Model Management Dashboard",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                }
            }

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TelemetryPill(label = "$runningCount RUNNING", color = EmeraldSignal)
                        TelemetryPill(label = "$stoppedCount STOPPED", color = CrimsonNeon)
                        TelemetryPill(label = "$totalModels TOTAL", color = ElectricCyan)
                    }
                    TextButton(
                        onClick = onOpenDeployDialog,
                        modifier = Modifier.testTag("dashboard_add_model_button")
                    ) {
                        Text("+ Deploy", color = AmberPulse, fontWeight = FontWeight.Bold)
                    }
                }

                // Aggregate VRAM & GPU Utilization Meters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Fleet VRAM Allocation",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "$totalVramUsed / $totalVramCapacity GB",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { vramRatio },
                            color = ElectricCyan,
                            trackColor = ObsidianBorder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Avg GPU Compute Load",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "$avgGpuUtilization%",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberPulse
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (avgGpuUtilization / 100f).coerceIn(0f, 1f) },
                            color = AmberPulse,
                            trackColor = ObsidianBorder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeployedModelDashboardCard(
    model: DeployedModelDoc,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReconfigure: () -> Unit,
    onSelectActive: () -> Unit,
    onPing: () -> Unit,
    onDelete: () -> Unit
) {
    val isRunning = model.status == "RUNNING" || model.status == "WARMING_UP"
    val statusColor = when (model.status) {
        "RUNNING" -> EmeraldSignal
        "WARMING_UP", "SCALING" -> AmberPulse
        else -> CrimsonNeon
    }

    val vramFraction = if (model.vramTotalGb > 0) {
        (model.vramUsedGb.toFloat() / model.vramTotalGb.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val gpuFraction = (model.gpuUtilizationPercent.toFloat() / 100f).coerceIn(0f, 1f)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (model.isSelected) ObsidianSurfaceElevated else ObsidianSurface
        ),
        border = BorderStroke(
            width = if (model.isSelected) 1.5.dp else 1.dp,
            color = if (model.isSelected) ElectricCyan else ObsidianBorder
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("model_card_${model.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Status Badge + Name + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = model.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        TelemetryPill(label = model.status, color = statusColor)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = model.modelRepoId,
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = model.endpointUrl,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("delete_model_${model.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Model Deployment",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Hardware & Quantization Tags
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryPill(label = model.provider.replace('_', ' '), color = AmberPulse)
                TelemetryPill(label = model.gpuTier, color = VioletWeight)
                TelemetryPill(label = model.quantization, color = ElectricCyan)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Resource Utilization Telemetry Box (VRAM, GPU Load, Active Workers, Throughput, Latency)
            Surface(
                color = Color(0xFF080C12),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "RESOURCE UTILIZATION & TELEMETRY",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan
                            )
                        }
                        Text(
                            text = "Workers: ${model.activeWorkers}/${model.maxWorkers} • ${model.requestsPerMin} req/m • ${model.avgLatencyMs}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary
                        )
                    }

                    // VRAM Progress Bar
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "VRAM Memory Usage",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "${model.vramUsedGb} GB / ${model.vramTotalGb} GB (${(vramFraction * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isRunning) EmeraldSignal else TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { vramFraction },
                            color = if (isRunning) EmeraldSignal else TextSecondary,
                            trackColor = ObsidianBorder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }

                    // GPU Compute Utilization Bar
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "GPU Compute Core Utilization",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "${model.gpuUtilizationPercent}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isRunning) AmberPulse else TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { gpuFraction },
                            color = if (isRunning) AmberPulse else TextSecondary,
                            trackColor = ObsidianBorder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }

                    Text(
                        text = "${model.weightMode}: ${model.abliterationLayerRange}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Model Lifecycle Controls: Start / Stop, Reconfigure, Ping, Connect for Duplex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRunning) {
                    FilledTonalButton(
                        onClick = onStop,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = CrimsonNeon.copy(alpha = 0.16f),
                            contentColor = CrimsonNeon
                        ),
                        border = BorderStroke(1.dp, CrimsonNeon.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("stop_model_button_${model.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Model",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onStart,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSignal,
                            contentColor = Color(0xFF04181C)
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("start_model_button_${model.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start Model",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onReconfigure,
                    border = BorderStroke(1.dp, AmberPulse.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("reconfigure_model_button_${model.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Reconfigure Model",
                        tint = AmberPulse,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reconfigure", color = AmberPulse)
                }

                OutlinedButton(
                    onClick = onPing,
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("ping_model_button_${model.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.NetworkPing,
                        contentDescription = "Ping Endpoint",
                        tint = ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ping", color = ElectricCyan)
                }

                Button(
                    onClick = onSelectActive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (model.isSelected) ElectricCyan.copy(alpha = 0.22f) else ElectricCyan,
                        contentColor = if (model.isSelected) ElectricCyan else Color(0xFF04181C)
                    ),
                    border = if (model.isSelected) BorderStroke(1.dp, ElectricCyan) else null,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("select_model_button_${model.id}")
                ) {
                    Icon(
                        imageVector = if (model.isSelected) Icons.Default.CheckCircle else Icons.Default.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (model.isSelected) "Active Target" else "Use in Duplex",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelConfigurationModal(
    title: String,
    initialModel: DeployedModelDoc?,
    onDismiss: () -> Unit,
    onConfirm: (
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
    ) -> Unit
) {
    var name by remember(initialModel) {
        mutableStateOf(initialModel?.name ?: "RunPod Llama-3.1-70B-Abliterated")
    }
    var provider by remember(initialModel) {
        mutableStateOf(initialModel?.provider ?: "RUNPOD_SERVERLESS")
    }
    var endpointUrl by remember(initialModel) {
        mutableStateOf(
            initialModel?.endpointUrl
                ?: "https://api.runpod.ai/v2/my-abliterated-vllm/runsync"
        )
    }
    var modelRepoId by remember(initialModel) {
        mutableStateOf(
            initialModel?.modelRepoId
                ?: "failspy/Meta-Llama-3-70B-Instruct-abliterated-v3.5"
        )
    }
    var gpuTier by remember(initialModel) {
        mutableStateOf(initialModel?.gpuTier ?: "2x NVIDIA A100 80GB SXM")
    }
    var quantization by remember(initialModel) {
        mutableStateOf(initialModel?.quantization ?: "EXL2 6.0bpw")
    }
    var weightMode by remember(initialModel) {
        mutableStateOf(initialModel?.weightMode ?: "ORTHOGONAL_ABLITERATED")
    }
    var abliterationLayers by remember(initialModel) {
        mutableStateOf(
            initialModel?.abliterationLayerRange
                ?: "Layers 14-29 Orthogonal Refusal Vector Zeroed"
        )
    }
    var maxWorkersText by remember(initialModel) {
        mutableStateOf((initialModel?.maxWorkers ?: 6L).toString())
    }
    var vramTotalText by remember(initialModel) {
        mutableStateOf((initialModel?.vramTotalGb ?: 160L).toString())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurfaceElevated,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = ElectricCyan
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = provider == "RUNPOD_SERVERLESS",
                        onClick = { provider = "RUNPOD_SERVERLESS" },
                        label = { Text("RunPod Serverless") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                            selectedLabelColor = ElectricCyan
                        ),
                        modifier = Modifier.testTag("modal_provider_runpod")
                    )
                    FilterChip(
                        selected = provider == "HUGGINGFACE_ENDPOINT",
                        onClick = { provider = "HUGGINGFACE_ENDPOINT" },
                        label = { Text("HF Endpoint") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberPulse.copy(alpha = 0.2f),
                            selectedLabelColor = AmberPulse
                        ),
                        modifier = Modifier.testTag("modal_provider_hf")
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Model Instance Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("modal_input_name")
                )

                OutlinedTextField(
                    value = modelRepoId,
                    onValueChange = { modelRepoId = it },
                    label = { Text("Model Weight Repo ID") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("modal_input_repo")
                )

                OutlinedTextField(
                    value = endpointUrl,
                    onValueChange = { endpointUrl = it },
                    label = { Text("Private Endpoint URL") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("modal_input_url")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = gpuTier,
                        onValueChange = { gpuTier = it },
                        label = { Text("GPU Tier") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = quantization,
                        onValueChange = { quantization = it },
                        label = { Text("Quantization") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = maxWorkersText,
                        onValueChange = { maxWorkersText = it },
                        label = { Text("Max Workers") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("modal_input_max_workers")
                    )
                    OutlinedTextField(
                        value = vramTotalText,
                        onValueChange = { vramTotalText = it },
                        label = { Text("Total VRAM (GB)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("modal_input_vram")
                    )
                }

                OutlinedTextField(
                    value = abliterationLayers,
                    onValueChange = { abliterationLayers = it },
                    label = { Text("Abliteration / Weight Adapter Spec") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val maxW = maxWorkersText.toLongOrNull()?.coerceIn(1L, 64L) ?: 4L
                    val vram = vramTotalText.toLongOrNull()?.coerceIn(8L, 2048L) ?: 80L
                    onConfirm(
                        name,
                        provider,
                        endpointUrl,
                        modelRepoId,
                        gpuTier,
                        quantization,
                        weightMode,
                        abliterationLayers,
                        maxW,
                        vram
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = Color(0xFF04181C)
                ),
                modifier = Modifier.testTag("modal_confirm_save_model")
            ) {
                Text(
                    text = if (initialModel == null) "Deploy Instance" else "Save Reconfiguration",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
