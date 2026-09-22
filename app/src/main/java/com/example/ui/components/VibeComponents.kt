package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.agent.AgentStage
import com.example.core.agent.TimelineNode
import com.example.core.router.AIMode
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
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

@Composable
fun VibeCyberCard(
    modifier: Modifier = Modifier,
    borderColor: Color = CyberBorder,
    backgroundColor: Color = CyberSurface,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        content()
    }
}

@Composable
fun VibeStatusPill(
    label: String,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = statusColor.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = statusColor
            )
        }
    }
}

@Composable
fun VibeHeaderBar(
    mode: AIMode,
    activeModelName: String,
    ramMbAvailable: Long,
    thermalStatus: String,
    onModeToggle: () -> Unit,
    onCreatorClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vibe_top_bar"),
        color = VoidBlack,
        border = BorderStroke(0.dp, Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo & Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(listOf(NeonCrimson, NeonViolet))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "V",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "VIBE",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = TextPrimary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "AI",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = NeonCrimson,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "by Shorif Uddin Piash",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = NeonCyan,
                            modifier = Modifier.clickable { onCreatorClick() }
                        )
                    }
                }

                // Mode switch pill
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onModeToggle() },
                        color = if (mode == AIMode.OFFLINE) NeonEmerald.copy(alpha = 0.15f) else NeonCyan.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (mode == AIMode.OFFLINE) NeonEmerald else NeonCyan)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (mode == AIMode.OFFLINE) NeonEmerald else NeonCyan)
                            )
                            Text(
                                text = if (mode == AIMode.OFFLINE) "OFFLINE" else "ONLINE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (mode == AIMode.OFFLINE) NeonEmerald else NeonCyan
                            )
                        }
                    }

                    // RAM pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = CyberSurfaceElevated,
                        border = BorderStroke(1.dp, CyberBorder)
                    ) {
                        Text(
                            text = "${ramMbAvailable}M RAM",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VibeCodeBlock(
    code: String,
    language: String = "kotlin",
    onOpenInEditor: (() -> Unit)? = null
) {
    val clipboard: ClipboardManager = LocalClipboardManager.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = VoidBlack,
        border = BorderStroke(1.dp, CyberBorder)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberSurfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.uppercase(),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { clipboard.setText(AnnotatedString(code)) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy code",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Text(
                text = code,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = TextPrimary,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

@Composable
fun VibeAgentTimelineView(
    nodes: List<TimelineNode>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        nodes.forEachIndexed { index, node ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status indicator node
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val nodeColor = when {
                        node.isCompleted -> NeonEmerald
                        node.isRunning -> NeonCyan
                        node.isFailed -> NeonCrimson
                        else -> TextMuted
                    }

                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(nodeColor.copy(alpha = 0.2f))
                            .border(1.5.dp, nodeColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (node.isCompleted) {
                            Text("✓", fontSize = 10.sp, color = NeonEmerald, fontWeight = FontWeight.Bold)
                        } else if (node.isRunning) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan)
                            )
                        }
                    }

                    if (index < nodes.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(26.dp)
                                .background(if (node.isCompleted) NeonEmerald.copy(alpha = 0.5f) else CyberBorder)
                        )
                    }
                }

                // Node content
                Column {
                    Text(
                        text = node.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (node.isCompleted || node.isRunning) TextPrimary else TextMuted
                    )
                    Text(
                        text = node.subtitle,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
