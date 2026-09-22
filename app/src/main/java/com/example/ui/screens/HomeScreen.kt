package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.router.AIMode
import com.example.ui.components.VibeCyberCard
import com.example.ui.components.VibeStatusPill
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack
import com.example.ui.viewmodel.VibeAIViewModel
import com.example.ui.viewmodel.VibeScreen

@Composable
fun HomeScreen(
    viewModel: VibeAIViewModel,
    modifier: Modifier = Modifier
) {
    val telemetry by viewModel.deviceCapability.collectAsState()
    val mode by viewModel.router.currentMode.collectAsState()
    val isLocalOnly by viewModel.router.is100PercentLocalPrivacy.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Cyberpunk Workstation Header
        item {
            VibeCyberCard(
                borderColor = NeonCrimson.copy(alpha = 0.6f),
                backgroundColor = CyberSurfaceElevated
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VibeStatusPill(
                            label = if (isLocalOnly) "100% LOCAL PRIVACY" else if (mode == AIMode.OFFLINE) "OFFLINE LLM ACTIVE" else "ONLINE CLOUD READY",
                            statusColor = if (isLocalOnly || mode == AIMode.OFFLINE) NeonEmerald else NeonCyan
                        )

                        Text(
                            text = "v2.5.0-PROD",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "VibeAI Workstation",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )

                    Text(
                        text = "Your Pocket AI Agent • Autonomous & Offline",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Creator Signature Pill
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.setCreatorDialogVisible(true) },
                        color = VoidBlack,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonViolet.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Architect: Shorif Uddin Piash (শরিফ উদ্দিন পিয়াস)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeonCyan
                            )
                        }
                    }
                }
            }
        }

        // Live Hardware & Thermal Telemetry HUD
        item {
            VibeCyberCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
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
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "System Telemetry & RAM HUD",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = "${telemetry.cpuCores} Cores | ${telemetry.thermalStatus}",
                            fontSize = 11.sp,
                            color = if (telemetry.thermalStatus == "Normal" || telemetry.thermalStatus == "Nominal") NeonEmerald else NeonAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "RAM: ${telemetry.availableRamMb} MB Free / ${telemetry.totalRamMb} MB",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "${telemetry.usedRamPercent}% Used",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (telemetry.usedRamPercent > 80) NeonAmber else NeonEmerald
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { telemetry.usedRamPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (telemetry.usedRamPercent > 80) NeonAmber else NeonCrimson,
                        trackColor = CyberBorder
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Model: ${viewModel.router.localEngine.activeModelName}",
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "VRAM: ${viewModel.router.localEngine.allocatedMemoryMb}M",
                            fontSize = 11.sp,
                            color = NeonCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Main Navigation Command Tiles
        item {
            Text(
                text = "WORKSTATION MODULES",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeModuleTile(
                        title = "Offline AI Chat",
                        subtitle = "Instant reasoning & code",
                        icon = Icons.Default.ChatBubble,
                        accentColor = NeonCrimson,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(VibeScreen.CHAT) }
                    )
                    HomeModuleTile(
                        title = "Agent Mode",
                        subtitle = "7-stage autonomous plan",
                        icon = Icons.Default.Psychology,
                        accentColor = NeonViolet,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(VibeScreen.AGENT) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeModuleTile(
                        title = "Code Workspace",
                        subtitle = "Editor & Termux Bridge",
                        icon = Icons.Default.Code,
                        accentColor = NeonCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(VibeScreen.CODE) }
                    )
                    HomeModuleTile(
                        title = "Files & Archives",
                        subtitle = "Safe zip, chunker & reader",
                        icon = Icons.Default.Folder,
                        accentColor = NeonEmerald,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(VibeScreen.FILES) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeModuleTile(
                        title = "GGUF Models",
                        subtitle = "Local models & benchmark",
                        icon = Icons.Default.DeveloperBoard,
                        accentColor = NeonAmber,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(VibeScreen.MODELS) }
                    )
                    HomeModuleTile(
                        title = "Media & Crypt",
                        subtitle = "AES-256 & media tools",
                        icon = Icons.Default.Lock,
                        accentColor = NeonViolet,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(VibeScreen.TOOLS) }
                    )
                }
            }
        }

        // Quick Command Prompts
        item {
            Text(
                text = "FAST COMMAND DISPATCH",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            val quickPrompts = listOf(
                "Find potential security flaws and race conditions in my project code",
                "Explain GGUF 4-bit quantization and mobile inference limits",
                "Run an autonomous Agent task: scan directory and build report",
                "How does VibeAI maintain 100% offline privacy?"
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                quickPrompts.forEach { q ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                viewModel.createNewConversation("Task: ${q.take(24)}...")
                                viewModel.sendMessage(q)
                            },
                        color = CyberSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = q,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeModuleTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        color = CyberSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 1
            )
        }
    }
}
