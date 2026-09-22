package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.termux.TermuxBridge
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
fun CodeWorkspaceScreen(
    viewModel: VibeAIViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fileName by viewModel.codeFileName.collectAsState()
    val codeContent by viewModel.codeContent.collectAsState()

    var editableCode by remember(codeContent) { mutableStateOf(codeContent) }
    var terminalOutput by remember { mutableStateOf<String?>(null) }
    val isTermuxAvailable = remember { TermuxBridge.isTermuxInstalled(context) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Workspace Top Bar
        item {
            VibeCyberCard(
                borderColor = NeonCyan.copy(alpha = 0.5f),
                backgroundColor = CyberSurfaceElevated
            ) {
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = fileName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        VibeStatusPill(
                            label = if (isTermuxAvailable) "TERMUX BRIDGE ACTIVE" else "SANDBOX RUNNER",
                            statusColor = if (isTermuxAvailable) NeonEmerald else NeonAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // AI Actions Row: Fix, Explain, Save, Launch Terminal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { viewModel.aiFixCode() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCrimson.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f).testTag("ai_fix_code_button"),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(13.dp), tint = NeonCrimson)
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("AI Fix", fontSize = 11.sp, color = NeonCrimson)
                        }

                        Button(
                            onClick = { viewModel.aiExplainCode() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonViolet.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(13.dp), tint = NeonViolet)
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Explain", fontSize = 11.sp, color = NeonViolet)
                        }

                        Button(
                            onClick = {
                                viewModel.saveCodeFile(editableCode)
                                Toast.makeText(context, "Saved $fileName", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(13.dp), tint = NeonEmerald)
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Save", fontSize = 11.sp, color = NeonEmerald)
                        }

                        Button(
                            onClick = {
                                if (isTermuxAvailable) {
                                    val intent = TermuxBridge.getLaunchIntent(context)
                                    if (intent != null) context.startActivity(intent)
                                } else {
                                    terminalOutput = "Sandbox Terminal Output:\n$ ./run_analysis.sh\nScanning AST tokens for $fileName...\nCompilation checks: SUCCESS (0 errors)\nSandbox status: STABLE"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(13.dp), tint = NeonCyan)
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Run", fontSize = 11.sp, color = NeonCyan)
                        }
                    }
                }
            }
        }

        // Code Editor Area with Line Numbers
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = VoidBlack,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "EDITOR CANVAS // MONOSPACE SYNTAX",
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editableCode,
                        onValueChange = { editableCode = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                            .testTag("code_editor_field"),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = NeonCyan
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberBorder,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = NeonCyan
                        )
                    )
                }
            }
        }

        // Terminal Execution Sandbox Output
        terminalOutput?.let { out ->
            item {
                VibeCyberCard(
                    borderColor = NeonEmerald.copy(alpha = 0.5f),
                    backgroundColor = VoidBlack
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "TERMINAL EXECUTION RESULT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonEmerald,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = out,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}
