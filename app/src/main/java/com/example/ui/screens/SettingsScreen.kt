package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.engine.offline.PerformanceProfile
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
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: VibeAIViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isLocalOnly by viewModel.router.is100PercentLocalPrivacy.collectAsState()
    val providers by viewModel.providerConfigs.collectAsState()

    var testConnectionStatus by remember { mutableStateOf<String?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }

    var selectedProfile by remember { mutableStateOf(PerformanceProfile.BALANCED) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Creator & Architect Spotlight
        item {
            VibeCyberCard(
                borderColor = NeonCyan.copy(alpha = 0.8f),
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
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeonCyan.copy(alpha = 0.15f))
                                    .border(1.dp, NeonCyan, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(
                                    text = "Shorif Uddin Piash",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "শরিফ উদ্দিন পিয়াস • Lead Architect",
                                    fontSize = 11.sp,
                                    color = NeonCyan
                                )
                            }
                        }

                        VibeStatusPill(label = "CREATOR", statusColor = NeonCyan)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "VibeAI was envisioned and designed by Shorif Uddin Piash to bring sovereign, offline-first artificial intelligence directly to mobile hardware without sacrificing modern agentic workflow capabilities.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // 100% Local Privacy Killswitch
        item {
            VibeCyberCard(
                borderColor = if (isLocalOnly) NeonEmerald else CyberBorder
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isLocalOnly) NeonEmerald.copy(alpha = 0.2f) else CyberSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isLocalOnly) NeonEmerald else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "100% Local Privacy Mode",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Completely disable network AI and enforce local GGUF only.",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Switch(
                        checked = isLocalOnly,
                        onCheckedChange = { viewModel.router.set100PercentLocalPrivacy(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonEmerald,
                            checkedTrackColor = NeonEmerald.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = CyberSurface
                        ),
                        modifier = Modifier.testTag("privacy_switch")
                    )
                }
            }
        }

        // Performance & Thermal Profiles
        item {
            VibeCyberCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(18.dp))
                        Text(
                            text = "PERFORMANCE & THERMAL PROFILE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    PerformanceProfile.values().forEach { profile ->
                        val isSelected = selectedProfile == profile
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedProfile = profile },
                            color = if (isSelected) CyberSurfaceElevated else CyberSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) NeonAmber else CyberBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = profile.displayName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) TextPrimary else TextSecondary
                                    )
                                    Text(
                                        text = "${profile.threads} CPU Threads • Max ${profile.maxTokens} tokens",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                if (isSelected) {
                                    VibeStatusPill(label = "ACTIVE", statusColor = NeonAmber)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Online OpenAI-Compatible Provider Config
        item {
            VibeCyberCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        Text(
                            text = "ONLINE AI PROVIDER CONFIGURATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val defaultProvider = providers.firstOrNull { it.isDefault } ?: providers.firstOrNull()
                    var baseUrlInput by remember(defaultProvider) { mutableStateOf(defaultProvider?.baseUrl ?: "https://api.openai.com/v1") }
                    var modelInput by remember(defaultProvider) { mutableStateOf(defaultProvider?.modelName ?: "gpt-4o-mini") }
                    var apiKeyInput by remember(defaultProvider) { mutableStateOf(defaultProvider?.apiKey ?: "") }

                    OutlinedTextField(
                        value = baseUrlInput,
                        onValueChange = { baseUrlInput = it },
                        label = { Text("Base URL", fontSize = 11.sp, color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, focusedTextColor = TextPrimary)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = modelInput,
                        onValueChange = { modelInput = it },
                        label = { Text("Model Name", fontSize = 11.sp, color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, focusedTextColor = TextPrimary)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("API Key (Encrypted in Keystore)", fontSize = 11.sp, color = TextMuted) },
                        placeholder = { Text("sk-...", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, focusedTextColor = TextPrimary)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.router.onlineClient.updateConfig(baseUrlInput, apiKeyInput)
                                isTestingConnection = true
                                testConnectionStatus = null
                                scope.launch {
                                    val res = viewModel.router.onlineClient.testConnection()
                                    isTestingConnection = false
                                    testConnectionStatus = if (res.isSuccess) "✓ ${res.getOrNull()}" else "✗ ${res.exceptionOrNull()?.message}"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isTestingConnection) "Testing..." else "Test Connection", fontSize = 11.sp, color = NeonCyan)
                        }

                        Button(
                            onClick = {
                                viewModel.router.onlineClient.updateConfig(baseUrlInput, apiKeyInput)
                                viewModel.saveProviderConfig("Custom Provider", baseUrlInput, apiKeyInput, modelInput)
                                Toast.makeText(context, "Provider configuration encrypted and saved!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Config", fontSize = 11.sp, color = DarkCanvas, fontWeight = FontWeight.Bold)
                        }
                    }

                    testConnectionStatus?.let { status ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = status,
                            fontSize = 11.sp,
                            color = if (status.startsWith("✓")) NeonEmerald else NeonCrimson,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
