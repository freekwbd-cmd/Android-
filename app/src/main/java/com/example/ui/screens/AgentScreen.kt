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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.agent.AgentStage
import com.example.ui.components.VibeAgentTimelineView
import com.example.ui.components.VibeCyberCard
import com.example.ui.components.VibeStatusPill
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.DarkCanvas
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

@Composable
fun AgentScreen(
    viewModel: VibeAIViewModel,
    modifier: Modifier = Modifier
) {
    val agentState by viewModel.agentRunner.state.collectAsState()
    val pendingPermission by viewModel.agentPermissionRequest.collectAsState()
    var targetGoal by remember { mutableStateOf("") }

    // Permission Dialog for dangerous actions (DELETE, WRITE, EXECUTE)
    pendingPermission?.let { req ->
        AlertDialog(
            onDismissRequest = { viewModel.respondToAgentPermission(false) },
            containerColor = CyberSurfaceElevated,
            icon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = NeonAmber,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Agent Permission Request",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tool: ${req.toolName}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Action: ${req.actionDescription}",
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Level: ${req.permissionLevel.name}",
                        fontSize = 11.sp,
                        color = NeonAmber,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.respondToAgentPermission(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald)
                ) {
                    Text("Allow Once", color = VoidBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.respondToAgentPermission(false) },
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCrimson)
                ) {
                    Text("Deny", color = NeonCrimson)
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Agent Status & Intro Banner
        item {
            VibeCyberCard(
                borderColor = NeonViolet.copy(alpha = 0.6f),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeonViolet.copy(alpha = 0.2f))
                                    .border(1.dp, NeonViolet, RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = NeonViolet,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "VibeAgent Autonomous Core",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        VibeStatusPill(
                            label = agentState.stage.name,
                            statusColor = when (agentState.stage) {
                                AgentStage.COMPLETED -> NeonEmerald
                                AgentStage.FAILED -> NeonCrimson
                                AgentStage.IDLE -> TextMuted
                                else -> NeonCyan
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "VibeAgent decomposes high-level developer objectives into verifiable tool actions with zero cloud dependencies and explicit user permission checkpoints.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // Agent Goal Input Box
        item {
            VibeCyberCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "ASSIGN AGENT OBJECTIVE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = targetGoal,
                        onValueChange = { targetGoal = it },
                        placeholder = {
                            Text(
                                text = "e.g. Scan project for bugs and export backup archive...",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("agent_goal_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonViolet,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Preset Goal suggestions
                        Text(
                            text = "Sample Objectives:",
                            fontSize = 11.sp,
                            color = TextMuted
                        )

                        if (agentState.stage != AgentStage.IDLE && agentState.stage != AgentStage.COMPLETED && agentState.stage != AgentStage.FAILED) {
                            Button(
                                onClick = { viewModel.agentRunner.stop() },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCrimson),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Stop Agent", fontSize = 12.sp)
                            }
                        } else {
                            Button(
                                onClick = {
                                    val goalToRun = targetGoal.ifBlank { "Scan project files, check code quality and verify safe sandbox status." }
                                    viewModel.runAgentGoal(goalToRun)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("run_agent_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Execute Objective", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset Chips
                    val presets = listOf(
                        "Scan project files for security bugs",
                        "Create project backup archive",
                        "Audit sandbox memory bounds"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        presets.forEach { preset ->
                            Text(
                                text = "• $preset",
                                fontSize = 11.sp,
                                color = NeonCyan,
                                modifier = Modifier
                                    .clickable { targetGoal = preset }
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Live 7-Stage Timeline
        if (agentState.timeline.isNotEmpty()) {
            item {
                VibeCyberCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "EXECUTION TIMELINE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        VibeAgentTimelineView(nodes = agentState.timeline)
                    }
                }
            }
        }

        // Final Report Card
        agentState.finalReport?.let { report ->
            item {
                VibeCyberCard(
                    borderColor = NeonEmerald.copy(alpha = 0.6f),
                    backgroundColor = CyberSurfaceElevated
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = NeonEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Objective Verification Report",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonEmerald
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = report,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            lineHeight = 17.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Live Activity Logs
        if (agentState.logs.isNotEmpty()) {
            item {
                VibeCyberCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "AGENT ACTIVITY LOGS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = VoidBlack,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                agentState.logs.takeLast(8).forEach { log ->
                                    Text(
                                        text = log,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (log.contains("GRANTED")) NeonEmerald else if (log.contains("DENIED")) NeonCrimson else TextSecondary,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
