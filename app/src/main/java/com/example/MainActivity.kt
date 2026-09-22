package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.VibeHeaderBar
import com.example.ui.components.VibeStatusPill
import com.example.ui.screens.AgentScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.CodeWorkspaceScreen
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ModelsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.GlassHighlight
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VibeAITheme
import com.example.ui.theme.VibeGradients
import com.example.ui.theme.VibeNeonMagenta
import com.example.ui.theme.VoidBlack
import com.example.ui.viewmodel.VibeAIViewModel
import com.example.ui.viewmodel.VibeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VibeAITheme {
                val viewModel: VibeAIViewModel = viewModel()
                VibeAIMainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun VibeAIMainApp(viewModel: VibeAIViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val telemetry by viewModel.deviceCapability.collectAsState()
    val mode by viewModel.router.currentMode.collectAsState()
    val showCreatorDialog by viewModel.showCreatorDialog.collectAsState()

    // Creator Spotlight Dialog
    if (showCreatorDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setCreatorDialogVisible(false) },
            containerColor = CyberSurfaceElevated,
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.15f))
                        .border(1.dp, NeonCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Shorif Uddin Piash",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = "শরিফ উদ্দিন পিয়াস • Creator & Lead Architect",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonCyan
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "VibeAI is designed as an elite, sovereign mobile AI workstation. It operates completely offline with on-device GGUF quantized neural inference, transparent 7-stage autonomous agent execution, secure AES-256 storage encryption, and professional developer tools.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = VoidBlack,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("• Vision: Pocket AI Agent with 100% Data Privacy", fontSize = 11.sp, color = TextPrimary)
                            Text("• Framework: Kotlin & Jetpack Compose (M3)", fontSize = 11.sp, color = TextPrimary)
                            Text("• Status: Silicon-Direct Neural Processing", fontSize = 11.sp, color = NeonEmerald)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.setCreatorDialogVisible(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCrimson)
                ) {
                    Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkCanvas,
        topBar = {
            VibeHeaderBar(
                mode = mode,
                activeModelName = viewModel.router.localEngine.activeModelName,
                ramMbAvailable = telemetry.availableRamMb,
                thermalStatus = telemetry.thermalStatus,
                onModeToggle = { viewModel.toggleAIMode() },
                onCreatorClick = { viewModel.setCreatorDialogVisible(true) }
            )
        },
        bottomBar = {
            VibeCyberBottomNav(
                currentScreen = currentScreen,
                onSelectScreen = { viewModel.navigateTo(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                VibeScreen.HOME -> HomeScreen(viewModel = viewModel)
                VibeScreen.CHAT -> ChatScreen(viewModel = viewModel)
                VibeScreen.AGENT -> AgentScreen(viewModel = viewModel)
                VibeScreen.FILES -> FilesScreen(viewModel = viewModel)
                VibeScreen.CODE -> CodeWorkspaceScreen(viewModel = viewModel)
                VibeScreen.MODELS -> ModelsScreen(viewModel = viewModel)
                VibeScreen.TOOLS -> ToolsScreen(viewModel = viewModel)
                VibeScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun VibeCyberBottomNav(
    currentScreen: VibeScreen,
    onSelectScreen: (VibeScreen) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("vibe_bottom_bar"),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(
                            GlassSurfaceElevated.copy(alpha = 0.92f),
                            VoidBlack.copy(alpha = 0.98f)
                        )
                    )
                )
                .border(
                    androidx.compose.foundation.BorderStroke(
                        1.dp,
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(
                                GlassHighlight,
                                GlassBorderCyan.copy(alpha = 0.35f)
                            )
                        )
                    )
                )
        ) {
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItem(
                    screen = VibeScreen.HOME,
                    label = "Home",
                    icon = Icons.Default.Home,
                    selected = currentScreen == VibeScreen.HOME,
                    accentColor = NeonCrimson,
                    onClick = { onSelectScreen(VibeScreen.HOME) }
                )
                NavItem(
                    screen = VibeScreen.CHAT,
                    label = "Chat",
                    icon = Icons.Default.ChatBubble,
                    selected = currentScreen == VibeScreen.CHAT,
                    accentColor = NeonCyan,
                    onClick = { onSelectScreen(VibeScreen.CHAT) }
                )
                NavItem(
                    screen = VibeScreen.AGENT,
                    label = "Agent",
                    icon = Icons.Default.Psychology,
                    selected = currentScreen == VibeScreen.AGENT,
                    accentColor = NeonViolet,
                    onClick = { onSelectScreen(VibeScreen.AGENT) }
                )
                NavItem(
                    screen = VibeScreen.FILES,
                    label = "Files",
                    icon = Icons.Default.Folder,
                    selected = currentScreen == VibeScreen.FILES,
                    accentColor = NeonEmerald,
                    onClick = { onSelectScreen(VibeScreen.FILES) }
                )
                NavItem(
                    screen = VibeScreen.CODE,
                    label = "Code",
                    icon = Icons.Default.Code,
                    selected = currentScreen == VibeScreen.CODE,
                    accentColor = VibeNeonMagenta,
                    onClick = { onSelectScreen(VibeScreen.CODE) }
                )
                NavItem(
                    screen = VibeScreen.MODELS,
                    label = "Models",
                    icon = Icons.Default.DeveloperBoard,
                    selected = currentScreen == VibeScreen.MODELS,
                    accentColor = NeonAmber,
                    onClick = { onSelectScreen(VibeScreen.MODELS) }
                )
                NavItem(
                    screen = VibeScreen.TOOLS,
                    label = "Tools",
                    icon = Icons.Default.Construction,
                    selected = currentScreen == VibeScreen.TOOLS,
                    accentColor = NeonViolet,
                    onClick = { onSelectScreen(VibeScreen.TOOLS) }
                )
                NavItem(
                    screen = VibeScreen.SETTINGS,
                    label = "Settings",
                    icon = Icons.Default.Settings,
                    selected = currentScreen == VibeScreen.SETTINGS,
                    accentColor = NeonCyan,
                    onClick = { onSelectScreen(VibeScreen.SETTINGS) }
                )
            }
        }
    }
}

@Composable
fun NavItem(
    screen: VibeScreen,
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .testTag("nav_item_${screen.name.lowercase()}"),
        color = if (selected) accentColor.copy(alpha = 0.20f) else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            if (selected) accentColor else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (selected) accentColor.copy(alpha = 0.25f) else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) accentColor else TextMuted,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (selected) accentColor else TextSecondary
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name! Welcome to VibeAI by Shorif Uddin Piash", modifier = modifier)
}

