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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LocalModelEntity
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
fun ModelsScreen(
    viewModel: VibeAIViewModel,
    modifier: Modifier = Modifier
) {
    val models by viewModel.localModels.collectAsState()
    val telemetry by viewModel.deviceCapability.collectAsState()
    val benchmark by viewModel.benchmarkResult.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Active Model Status Banner
        item {
            VibeCyberCard(
                borderColor = NeonAmber.copy(alpha = 0.6f),
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
                        Text(
                            text = "LOCAL MODEL WORKSTATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )

                        VibeStatusPill(
                            label = if (viewModel.router.localEngine.isModelLoaded) "LOADED IN VRAM" else "UNLOADED",
                            statusColor = if (viewModel.router.localEngine.isModelLoaded) NeonEmerald else NeonAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = viewModel.router.localEngine.activeModelName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = "VRAM: ${viewModel.router.localEngine.allocatedMemoryMb} MB | Backend: GGUF / llama.cpp | 4-bit Quantized",
                        fontSize = 11.sp,
                        color = NeonCyan,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.runQuickBenchmark(viewModel.router.localEngine.activeModelName) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f).testTag("quick_benchmark_button")
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp), tint = NeonCyan)
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Benchmark", fontSize = 11.sp, color = NeonCyan)
                        }

                        if (viewModel.router.localEngine.isModelLoaded) {
                            Button(
                                onClick = { viewModel.unloadActiveModel() },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCrimson.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = NeonCrimson)
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Unload", fontSize = 11.sp, color = NeonCrimson)
                            }
                        }
                    }
                }
            }
        }

        // Benchmark Result HUD
        benchmark?.let { b ->
            item {
                VibeCyberCard(
                    borderColor = NeonCyan.copy(alpha = 0.8f),
                    backgroundColor = VoidBlack
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Text(
                                text = "BENCHMARK PERFORMANCE HUD",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Generation Speed", fontSize = 11.sp, color = TextMuted)
                                Text("${b.tokensPerSec} tok/s", fontSize = 16.sp, fontWeight = FontWeight.Black, color = NeonEmerald, fontFamily = FontFamily.Monospace)
                            }
                            Column {
                                Text("First Token Latency", fontSize = 11.sp, color = TextMuted)
                                Text("${b.firstTokenLatencyMs} ms", fontSize = 16.sp, fontWeight = FontWeight.Black, color = NeonCyan, fontFamily = FontFamily.Monospace)
                            }
                            Column {
                                Text("Memory Allocated", fontSize = 11.sp, color = TextMuted)
                                Text("${b.ramUsedMb} MB", fontSize = 16.sp, fontWeight = FontWeight.Black, color = NeonAmber, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        // Model List Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GGUF MODEL CATALOG",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "${models.size} Available",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }

        items(models, key = { it.id }) { model ->
            LocalModelCard(
                model = model,
                isRecommendedForDevice = model.ramEstimateMb < telemetry.availableRamMb,
                onLoad = { viewModel.loadModel(model) }
            )
        }
    }
}

@Composable
fun LocalModelCard(
    model: LocalModelEntity,
    isRecommendedForDevice: Boolean,
    onLoad: () -> Unit
) {
    val isActive = model.status == "ACTIVE"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CyberSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) NeonEmerald else CyberBorder
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = model.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${model.parameterSize} params • ${model.quantization} • ${model.fileSizeMb} MB",
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (isActive) {
                    VibeStatusPill(label = "ACTIVE", statusColor = NeonEmerald)
                } else if (!isRecommendedForDevice) {
                    VibeStatusPill(label = "HIGH RAM", statusColor = NeonAmber)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Est. RAM: ${model.ramEstimateMb} MB | Context: ${model.contextLength}",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )

                if (!isActive) {
                    Button(
                        onClick = onLoad,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceElevated),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("Load", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
