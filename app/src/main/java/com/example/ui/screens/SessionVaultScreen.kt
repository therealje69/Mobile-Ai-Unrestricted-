package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ConversationSessionEntity
import com.example.ui.theme.AmberPulse
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SessionVaultScreen(
    sessions: List<ConversationSessionEntity>,
    activeSessionId: Long?,
    onSelectSession: (ConversationSessionEntity) -> Unit,
    onStartFreshSession: () -> Unit,
    onDeleteSession: (ConversationSessionEntity) -> Unit,
    onBuildExportPayload: suspend (ConversationSessionEntity, Boolean) -> String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportedPreviewTitle by remember { mutableStateOf<String?>(null) }
    var exportedPreviewContent by remember { mutableStateOf("") }
    val sdf = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.US) }

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
                .testTag("session_vault_list"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = EmeraldSignal,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "DEVICE-LOCAL ROOM SQLITE VAULT",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldSignal
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Zero Cloud Logging • Exportable Session Logs",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "All unfiltered duplex transcripts, latency telemetry, and weight configs remain strictly on your device.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onStartFreshSession,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = Color(0xFF04181C)
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("vault_new_session_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            items(sessions, key = { it.id }) { session ->
                val isCurrent = session.id == activeSessionId
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) ObsidianSurfaceElevated else ObsidianSurface
                    ),
                    border = BorderStroke(
                        width = if (isCurrent) 1.5.dp else 1.dp,
                        color = if (isCurrent) ElectricCyan else ObsidianBorder
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("session_card_${session.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = session.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Node: ${session.deploymentName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricCyan,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Persona: ${session.personaName} • Updated ${sdf.format(Date(session.updatedAt))}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }

                            IconButton(
                                onClick = { onDeleteSession(session) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("delete_session_${session.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Session",
                                    tint = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onSelectSession(session) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCurrent) EmeraldSignal else ElectricCyan,
                                    contentColor = Color(0xFF04181C)
                                ),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("open_session_${session.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isCurrent) "Active" else "Load",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        val payload = onBuildExportPayload(session, true)
                                        exportedPreviewTitle = "${session.title} (JSON)"
                                        exportedPreviewContent = payload
                                    }
                                },
                                border = BorderStroke(1.dp, AmberPulse.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("export_json_${session.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DataObject,
                                    contentDescription = null,
                                    tint = AmberPulse,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("JSON", color = AmberPulse)
                            }

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        val payload = onBuildExportPayload(session, false)
                                        exportedPreviewTitle = "${session.title} (Markdown)"
                                        exportedPreviewContent = payload
                                    }
                                },
                                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("export_markdown_${session.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("MD", color = ElectricCyan)
                            }
                        }
                    }
                }
            }
        }
    }

    if (exportedPreviewTitle != null) {
        AlertDialog(
            onDismissRequest = { exportedPreviewTitle = null },
            containerColor = ObsidianSurfaceElevated,
            title = {
                Text(
                    text = exportedPreviewTitle.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = ElectricCyan
                )
            },
            text = {
                Surface(
                    color = Color(0xFF070A0E),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = exportedPreviewContent,
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldSignal,
                        modifier = Modifier.padding(12.dp),
                        maxLines = 18,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, exportedPreviewTitle)
                            putExtra(Intent.EXTRA_TEXT, exportedPreviewContent)
                        }
                        context.startActivity(
                            Intent.createChooser(sendIntent, "Export Sovereign Session Log")
                        )
                        exportedPreviewTitle = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = Color(0xFF04181C)
                    ),
                    modifier = Modifier.testTag("share_exported_log_button")
                ) {
                    Text("Share / Save File", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { exportedPreviewTitle = null },
                    modifier = Modifier.testTag("close_export_preview_button")
                ) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }
}
