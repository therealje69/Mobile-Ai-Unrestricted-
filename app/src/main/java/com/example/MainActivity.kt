package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.SovereignTab
import com.example.ui.SovereignVoiceViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.screens.DuplexLiveScreen
import com.example.ui.screens.ModelManagementDashboardScreen
import com.example.ui.screens.SessionVaultScreen
import com.example.ui.screens.WeightTuningStudioScreen
import com.example.ui.theme.AmberPulse
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppAuthGate()
            }
        }
    }
}

@Composable
fun AppAuthGate() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        SovereignVoiceApp(
            currentUserId = user.uid,
            onSignOut = {
                currentUser = null
            }
        )
    }
}

private data class NavDestination(
    val tab: SovereignTab,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SovereignVoiceApp(
    currentUserId: String,
    onSignOut: () -> Unit,
    viewModel: SovereignVoiceViewModel = viewModel(
        key = currentUserId,
        factory = SovereignVoiceViewModel.factory(currentUserId)
    )
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val deployedModels by viewModel.deployedModels.collectAsStateWithLifecycle()
    val personas by viewModel.personas.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val activeSessionId by viewModel.activeSessionId.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val isHandsFreeActive by viewModel.isHandsFreeDuplexActive.collectAsStateWithLifecycle()
    val rmsEnergy by viewModel.rmsEnergyLevel.collectAsStateWithLifecycle()
    val spectrumBands by viewModel.spectrumBands.collectAsStateWithLifecycle()
    val livePartialTranscript by viewModel.livePartialTranscript.collectAsStateWithLifecycle()
    val statusTelemetry by viewModel.statusTelemetry.collectAsStateWithLifecycle()
    val lastRawPayload by viewModel.lastRawPayloadPreview.collectAsStateWithLifecycle()
    val bannerNotification by viewModel.bannerNotification.collectAsStateWithLifecycle()

    val activeDeploymentEntity = remember(deployedModels) {
        viewModel.getActiveDeploymentAsEntity()
    }
    val activePersona = personas.firstOrNull { it.isSelected } ?: personas.firstOrNull()

    if (selectedTab != SovereignTab.DUPLEX_LIVE) {
        BackHandler {
            viewModel.selectTab(SovereignTab.DUPLEX_LIVE)
        }
    }

    val destinations = listOf(
        NavDestination(
            tab = SovereignTab.DUPLEX_LIVE,
            label = "Live Duplex",
            icon = Icons.Default.GraphicEq,
            testTag = "nav_tab_duplex"
        ),
        NavDestination(
            tab = SovereignTab.MODEL_DASHBOARD,
            label = "Model Fleet",
            icon = Icons.Default.Dashboard,
            testTag = "nav_tab_models"
        ),
        NavDestination(
            tab = SovereignTab.WEIGHT_STUDIO,
            label = "Weight Studio",
            icon = Icons.Default.Tune,
            testTag = "nav_tab_tuning"
        ),
        NavDestination(
            tab = SovereignTab.SESSION_VAULT,
            label = "Session Vault",
            icon = Icons.Default.History,
            testTag = "nav_tab_vault"
        )
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = ObsidianBackground,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ObsidianBackground,
                        titleContentColor = TextPrimary
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(ElectricCyan)
                            )
                            Column {
                                Text(
                                    text = "SOVEREIGN VOICE",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "PRIVATE ABLITERATED / WEIGHTED DUPLEX CONSOLE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AmberPulse
                                )
                            }
                        }
                    },
                    actions = {
                        Surface(
                            color = ObsidianSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ObsidianBorder),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = if (isHandsFreeActive) "VAD LIVE" else "DIRECT RAW",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isHandsFreeActive) EmeraldSignal else ElectricCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                signOutUser(
                                    context = context,
                                    onSignOutComplete = onSignOut,
                                    scope = scope
                                )
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign Out",
                                tint = TextSecondary
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        tonalElevation = 0.dp
                    ) {
                        destinations.forEach { dest ->
                            val isSelected = selectedTab == dest.tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                icon = {
                                    Icon(
                                        imageVector = dest.icon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.label,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF04181C),
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = ElectricCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = ObsidianSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        destinations.forEach { dest ->
                            val isSelected = selectedTab == dest.tab
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                icon = {
                                    Icon(
                                        imageVector = dest.icon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.label,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Color(0xFF04181C),
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = ElectricCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    AnimatedVisibility(visible = bannerNotification != null) {
                        Surface(
                            color = ObsidianSurfaceElevated,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = bannerNotification.orEmpty(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricCyan,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { viewModel.dismissBanner() }) {
                                    Text("OK", color = AmberPulse)
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTab) {
                            SovereignTab.DUPLEX_LIVE -> DuplexLiveScreen(
                                activeDeployment = activeDeploymentEntity,
                                activePersona = activePersona,
                                messages = activeMessages,
                                voiceState = voiceState,
                                isHandsFreeActive = isHandsFreeActive,
                                rmsEnergy = rmsEnergy,
                                spectrumBands = spectrumBands,
                                livePartialTranscript = livePartialTranscript,
                                statusTelemetry = statusTelemetry,
                                lastRawPayloadPreview = lastRawPayload,
                                onToggleHandsFreeDuplex = { viewModel.toggleHandsFreeDuplex() },
                                onTriggerBargeIn = { prompt -> viewModel.triggerManualBargeIn(prompt) },
                                onSendTurn = { text, isVoice -> viewModel.sendDuplexTurn(text, isVoice) },
                                onStartFreshSession = { viewModel.startFreshDuplexSession() },
                                onReplayVoice = { text -> viewModel.voiceController.speakModelResponse(text) },
                                onShowNotification = { msg -> viewModel.showNotification(msg) }
                            )

                            SovereignTab.MODEL_DASHBOARD -> ModelManagementDashboardScreen(
                                models = deployedModels,
                                onStartModel = { viewModel.startCloudModel(it) },
                                onStopModel = { viewModel.stopCloudModel(it) },
                                onSelectActiveModel = { viewModel.selectActiveCloudModel(it) },
                                onPingModel = { viewModel.pingCloudModelHealth(it) },
                                onDeleteModel = { viewModel.deleteCloudModel(it) },
                                onReconfigureModel = { id, name, prov, url, repo, gpu, quant, mode, layers, maxW, vram ->
                                    viewModel.reconfigureCloudModel(
                                        id, name, prov, url, repo, gpu, quant, mode, layers, maxW, vram
                                    )
                                },
                                onDeployNewModel = { name, prov, url, repo, gpu, quant, mode, layers, maxW, vram ->
                                    viewModel.deployNewCloudModel(
                                        name, prov, url, repo, gpu, quant, mode, layers, maxW, vram
                                    )
                                }
                            )

                            SovereignTab.WEIGHT_STUDIO -> WeightTuningStudioScreen(
                                personas = personas,
                                activePersona = activePersona,
                                onSelectPersona = { viewModel.selectPersona(it) },
                                onUpdatePersona = { viewModel.updateActivePersonaParameters(it) },
                                onApplyQuickPreset = { viewModel.applyQuickWeightPreset(it) },
                                onCreatePersona = { name, tag, prompt, ablit, lora, temp, pitch, rate ->
                                    viewModel.createNewPersona(name, tag, prompt, ablit, lora, temp, pitch, rate)
                                }
                            )

                            SovereignTab.SESSION_VAULT -> SessionVaultScreen(
                                sessions = sessions,
                                activeSessionId = activeSessionId,
                                onSelectSession = { session ->
                                    viewModel.selectSession(session)
                                    viewModel.selectTab(SovereignTab.DUPLEX_LIVE)
                                },
                                onStartFreshSession = {
                                    viewModel.startFreshDuplexSession()
                                    viewModel.selectTab(SovereignTab.DUPLEX_LIVE)
                                },
                                onDeleteSession = { viewModel.deleteSession(it) },
                                onBuildExportPayload = { session, asJson ->
                                    viewModel.buildExportPayload(session, asJson)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
