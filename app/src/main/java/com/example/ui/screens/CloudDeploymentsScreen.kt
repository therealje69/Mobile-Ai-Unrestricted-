package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.local.CloudDeploymentEntity
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
fun CloudDeploymentsScreen(
    deployments: List<CloudDeploymentEntity>,
    onSelectDeployment: (CloudDeploymentEntity) -> Unit,
    onScaleWorkers: (CloudDeploymentEntity, Int) -> Unit,
    onPingHealth: (CloudDeploymentEntity) -> Unit,
    onDeleteDeployment: (CloudDeploymentEntity) -> Unit,
    onDeployNewNode: (
        name: String,
        provider: String,
        endpointUrl: String,
        modelRepoId: String,
        gpuTier: String,
        quantization: String,
        weightMode: String,
        abliterationLayerRange: String,
        maxWorkers: Int,
        customAuthHeader: String
    ) -> Unit
) {
    var showDeployModal by remember { mutableStateOf(false) }

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
                .testTag("deployments_list"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 88.dp)
        ) {
            item {
                CloudInfrastructureHeaderCard(
                    totalNodes = deployments.size,
                    activeWorkers = deployments.sumOf { it.activeWorkers },
                    onOpenDeployDialog = { showDeployModal = true }
                )
            }

            items(deployments, key = { it.id }) { node ->
                CloudDeploymentNodeCard(
                    node = node,
                    onSelect = { onSelectDeployment(node) },
                    onScaleUp = { onScaleWorkers(node, 1) },
                    onScaleDown = { onScaleWorkers(node, -1) },
                    onPing = { onPingHealth(node) },
                    onDelete = { onDeleteDeployment(node) }
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
                .testTag("fab_deploy_new_node")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Deploy Private Node")
                Text(
                    text = "Deploy Node",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showDeployModal) {
        DeployNewNodeDialog(
            onDismiss = { showDeployModal = false },
            onConfirmDeploy = { name, provider, url, repo, gpu, quant, weightMode, layers, maxW, auth ->
                onDeployNewNode(name, provider, url, repo, gpu, quant, weightMode, layers, maxW, auth)
                showDeployModal = false
            }
        )
    }
}

@Composable
private fun CloudInfrastructureHeaderCard(
    totalNodes: Int,
    activeWorkers: Int,
    onOpenDeployDialog: () -> Unit
) {
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
                    .height(110.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_cloud_node_hero),
                    contentDescription = "Private Cloud Compute Infrastructure",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "PRIVATE GPU ORCHESTRATOR",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                    Text(
                        text = "RunPod Serverless & HuggingFace Endpoints",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TelemetryPill(label = "$totalNodes PRIVATE NODES", color = ElectricCyan)
                    TelemetryPill(label = "$activeWorkers ACTIVE GPU WORKERS", color = EmeraldSignal)
                }

                TextButton(
                    onClick = onOpenDeployDialog,
                    modifier = Modifier.testTag("header_deploy_node_button")
                ) {
                    Text("+ New Endpoint", color = AmberPulse)
                }
            }
        }
    }
}

@Composable
private fun CloudDeploymentNodeCard(
    node: CloudDeploymentEntity,
    onSelect: () -> Unit,
    onScaleUp: () -> Unit,
    onScaleDown: () -> Unit,
    onPing: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (node.status) {
        "ONLINE" -> EmeraldSignal
        "WARMING_UP", "SCALING" -> AmberPulse
        else -> TextSecondary
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (node.isSelected) ObsidianSurfaceElevated else ObsidianSurface
        ),
        border = BorderStroke(
            width = if (node.isSelected) 1.5.dp else 1.dp,
            color = if (node.isSelected) ElectricCyan else ObsidianBorder
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("deployment_card_${node.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = node.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = node.modelRepoId,
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = node.endpointUrl,
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
                        .testTag("delete_node_${node.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Remove Node",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryPill(label = node.provider.replace('_', ' '), color = AmberPulse)
                TelemetryPill(label = node.gpuTier, color = VioletWeight)
                TelemetryPill(label = node.quantization, color = ElectricCyan)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = Color(0xFF090D14),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "WEIGHT ARCHITECTURE: ${node.weightMode}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberPulse
                    )
                    Text(
                        text = node.abliterationLayerRange,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Worker Scaling & Ping Controls
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
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Workers: ${node.activeWorkers}/${node.maxWorkers} • ${node.avgLatencyMs}ms",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                    IconButton(
                        onClick = onScaleDown,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("scale_down_${node.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Scale Down Worker",
                            tint = TextSecondary
                        )
                    }
                    IconButton(
                        onClick = onScaleUp,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("scale_up_${node.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Scale Up / Wake Worker",
                            tint = EmeraldSignal
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onPing,
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("ping_node_${node.id}")
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
                        onClick = onSelect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (node.isSelected) EmeraldSignal else ElectricCyan,
                            contentColor = Color(0xFF04181C)
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("select_node_${node.id}")
                    ) {
                        Icon(
                            imageVector = if (node.isSelected) Icons.Default.CheckCircle else Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (node.isSelected) "Active" else "Connect",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeployNewNodeDialog(
    onDismiss: () -> Unit,
    onConfirmDeploy: (
        name: String,
        provider: String,
        endpointUrl: String,
        modelRepoId: String,
        gpuTier: String,
        quantization: String,
        weightMode: String,
        abliterationLayerRange: String,
        maxWorkers: Int,
        customAuthHeader: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("RunPod Llama-3.1-70B-Abliterated") }
    var provider by remember { mutableStateOf("RUNPOD_SERVERLESS") }
    var endpointUrl by remember {
        mutableStateOf("https://api.runpod.ai/v2/my-abliterated-vllm/runsync")
    }
    var modelRepoId by remember {
        mutableStateOf("failspy/Meta-Llama-3-70B-Instruct-abliterated-v3.5")
    }
    var gpuTier by remember { mutableStateOf("2x NVIDIA A100 80GB SXM") }
    var quantization by remember { mutableStateOf("EXL2 6.0bpw") }
    var weightMode by remember { mutableStateOf("ORTHOGONAL_ABLITERATED") }
    var abliterationLayers by remember {
        mutableStateOf("Layers 14-29 Orthogonal Refusal Vector Zeroed")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurfaceElevated,
        title = {
            Text(
                text = "Deploy Private Cloud Model Node",
                style = MaterialTheme.typography.titleLarge,
                color = ElectricCyan
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = provider == "RUNPOD_SERVERLESS",
                        onClick = {
                            provider = "RUNPOD_SERVERLESS"
                            endpointUrl = "https://api.runpod.ai/v2/my-abliterated-vllm/runsync"
                        },
                        label = { Text("RunPod Serverless") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                            selectedLabelColor = ElectricCyan
                        ),
                        modifier = Modifier.testTag("chip_provider_runpod")
                    )
                    FilterChip(
                        selected = provider == "HUGGINGFACE_ENDPOINT",
                        onClick = {
                            provider = "HUGGINGFACE_ENDPOINT"
                            endpointUrl = "https://custom-ablit.us-east-1.aws.endpoints.huggingface.cloud/v1/chat/completions"
                        },
                        label = { Text("HF Endpoint") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberPulse.copy(alpha = 0.2f),
                            selectedLabelColor = AmberPulse
                        ),
                        modifier = Modifier.testTag("chip_provider_hf")
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Deployment Node Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_node_name")
                )

                OutlinedTextField(
                    value = modelRepoId,
                    onValueChange = { modelRepoId = it },
                    label = { Text("HuggingFace Model / Weight Repo ID") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_model_repo")
                )

                OutlinedTextField(
                    value = endpointUrl,
                    onValueChange = { endpointUrl = it },
                    label = { Text("Serverless Endpoint URL") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_endpoint_url")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = gpuTier,
                        onValueChange = { gpuTier = it },
                        label = { Text("GPU Instance") },
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
                    onConfirmDeploy(
                        name,
                        provider,
                        endpointUrl,
                        modelRepoId,
                        gpuTier,
                        quantization,
                        weightMode,
                        abliterationLayers,
                        4,
                        ""
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = Color(0xFF04181C)
                ),
                modifier = Modifier.testTag("confirm_deploy_node_button")
            ) {
                Text("Launch Node", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
