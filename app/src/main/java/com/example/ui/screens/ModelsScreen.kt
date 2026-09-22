package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.network.DownloadProgress
import com.example.core.network.DownloadStatus
import com.example.core.network.ModelDownloader
import com.example.core.network.OnlineModelPreset
import com.example.data.model.LocalModelEntity
import com.example.ui.components.VibeGlassCard
import com.example.ui.components.VibeGlowingIcon
import com.example.ui.components.VibeStatusPill
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.GlassBorderAmber
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.GlassBorderEmerald
import com.example.ui.theme.GlassBorderMagenta
import com.example.ui.theme.GlassBorderViolet
import com.example.ui.theme.GlassHighlight
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.NeonVioletLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VibeGradients
import com.example.ui.theme.VibeNeonMagenta
import com.example.ui.theme.VoidBlack
import com.example.ui.viewmodel.VibeAIViewModel
import com.example.ui.viewmodel.VibeScreen
import java.io.File
import java.util.Locale

@Composable
fun ModelsScreen(
    viewModel: VibeAIViewModel,
    modifier: Modifier = Modifier
) {
    val models by viewModel.localModels.collectAsState()
    val telemetry by viewModel.deviceCapability.collectAsState()
    val benchmark by viewModel.benchmarkResult.collectAsState()
    val validation by viewModel.importedModelValidation.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val importProgress by viewModel.importProgress.collectAsState()
    val scanStatusMessage by viewModel.scanStatusMessage.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importGGUFFromUri(it) }
    }

    var customUrlInput by remember { mutableStateOf("") }
    var customNameInput by remember { mutableStateOf("") }
    var customPathInput by remember { mutableStateOf("") }
    var showCustomUrlCard by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Active Model Status Glass Hero Card
        item {
            VibeGlassCard(
                borderColor = if (viewModel.router.localEngine.isModelLoaded) GlassBorderEmerald else GlassBorderAmber,
                backgroundColor = GlassSurfaceElevated
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
                            VibeGlowingIcon(
                                icon = Icons.Default.Memory,
                                tint = if (viewModel.router.localEngine.isModelLoaded) NeonEmerald else NeonAmber,
                                size = 32.dp,
                                iconSize = 18.dp
                            )
                            Column {
                                Text(
                                    text = "NEURAL INFERENCE ENGINE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = if (viewModel.router.localEngine.isModelLoaded) "VRAM ALLOCATED" else "STANDBY / UNLOADED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (viewModel.router.localEngine.isModelLoaded) NeonEmerald else NeonAmber
                                )
                            }
                        }

                        VibeStatusPill(
                            label = if (viewModel.router.localEngine.isModelLoaded) "ACTIVE" else "READY",
                            statusColor = if (viewModel.router.localEngine.isModelLoaded) NeonEmerald else NeonAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = viewModel.router.localEngine.activeModelName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "RAM Used: ${viewModel.router.localEngine.allocatedMemoryMb} MB",
                            fontSize = 11.sp,
                            color = NeonCyan,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Device Free: ${telemetry.availableRamMb} MB",
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.runQuickBenchmark(viewModel.router.localEngine.activeModelName) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_benchmark_button")
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(15.dp), tint = NeonCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run Benchmark", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                        }

                        if (viewModel.router.localEngine.isModelLoaded) {
                            Button(
                                onClick = { viewModel.unloadActiveModel() },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCrimson.copy(alpha = 0.18f)),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, NeonCrimson.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("unload_model_button")
                            ) {
                                Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(15.dp), tint = NeonCrimson)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Unload", fontSize = 11.sp, color = NeonCrimson, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Live Benchmark Telemetry Glass HUD
        benchmark?.let { b ->
            item {
                VibeGlassCard(
                    borderColor = GlassBorderCyan,
                    backgroundColor = VoidBlack
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "REAL BENCHMARK TELEMETRY",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }
                            Text(
                                text = b.modelName,
                                fontSize = 11.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Generation Speed", fontSize = 11.sp, color = TextMuted)
                                Text(
                                    text = String.format(Locale.US, "%.1f tok/s", b.tokensPerSec),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NeonEmerald,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Column {
                                Text("Latency", fontSize = 11.sp, color = TextMuted)
                                Text("${b.firstTokenLatencyMs} ms", fontSize = 16.sp, fontWeight = FontWeight.Black, color = NeonCyan, fontFamily = FontFamily.Monospace)
                            }
                            Column {
                                Text("RAM Allocated", fontSize = 11.sp, color = TextMuted)
                                Text("${b.ramUsedMb} MB", fontSize = 16.sp, fontWeight = FontWeight.Black, color = NeonAmber, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        // DIRECT ONLINE DOWNLOAD ACTIVE PROGRESS HUD
        if (downloadProgress.status != DownloadStatus.IDLE) {
            item {
                val isDownloading = downloadProgress.status == DownloadStatus.DOWNLOADING || downloadProgress.status == DownloadStatus.CONNECTING || downloadProgress.status == DownloadStatus.VALIDATING
                val isCompleted = downloadProgress.status == DownloadStatus.COMPLETED
                val isFailed = downloadProgress.status == DownloadStatus.FAILED || downloadProgress.status == DownloadStatus.CANCELLED

                VibeGlassCard(
                    borderColor = when {
                        isCompleted -> GlassBorderEmerald
                        isFailed -> GlassBorderMagenta
                        else -> GlassBorderCyan
                    },
                    backgroundColor = GlassSurfaceElevated
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
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
                                    imageVector = if (isCompleted) Icons.Default.Check else Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = if (isCompleted) NeonEmerald else if (isFailed) NeonCrimson else NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "ONLINE DOWNLOAD: ${downloadProgress.modelName}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = when (downloadProgress.status) {
                                            DownloadStatus.CONNECTING -> "Connecting to remote HuggingFace server..."
                                            DownloadStatus.DOWNLOADING -> "Downloading model chunks directly..."
                                            DownloadStatus.VALIDATING -> "Validating GGUF binary magic & tensors..."
                                            DownloadStatus.COMPLETED -> "✓ Successfully installed & registered!"
                                            DownloadStatus.FAILED -> "✗ Error: ${downloadProgress.errorMessage}"
                                            DownloadStatus.CANCELLED -> "Download cancelled"
                                            else -> ""
                                        },
                                        fontSize = 10.sp,
                                        color = if (isCompleted) NeonEmerald else if (isFailed) NeonCrimson else NeonCyan
                                    )
                                }
                            }

                            if (isDownloading) {
                                IconButton(
                                    onClick = { viewModel.cancelModelDownload() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Cancel, contentDescription = "Cancel", tint = NeonCrimson, modifier = Modifier.size(18.dp))
                                }
                            } else {
                                IconButton(
                                    onClick = { viewModel.dismissDownloadStatus() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Cancel, contentDescription = "Dismiss", tint = TextMuted, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        if (isDownloading) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { downloadProgress.progressPercent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = NeonCyan,
                                trackColor = CyberBorder
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val dlMb = downloadProgress.downloadedBytes / (1024 * 1024)
                                val totMb = downloadProgress.totalBytes / (1024 * 1024)
                                val speedMb = downloadProgress.speedBytesPerSec.toFloat() / (1024f * 1024f)

                                Text(
                                    text = "$dlMb MB / ${if (totMb > 0) "$totMb MB" else "..."} (${(downloadProgress.progressPercent * 100).toInt()}%)",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary
                                )

                                Text(
                                    text = String.format(Locale.US, "%.2f MB/s", speedMb),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = NeonEmerald
                                )
                            }
                        }
                    }
                }
            }
        }

        // Device File Import Progress Card
        if (importProgress != null) {
            val imp = importProgress!!
            item {
                VibeGlassCard(
                    borderColor = when {
                        imp.isSuccess -> GlassBorderEmerald
                        imp.error != null -> GlassBorderMagenta
                        else -> GlassBorderViolet
                    },
                    backgroundColor = GlassSurfaceElevated
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (imp.isSuccess) Icons.Default.Check else if (imp.error != null) Icons.Default.Cancel else Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = if (imp.isSuccess) NeonEmerald else if (imp.error != null) NeonCrimson else NeonViolet,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "LOCAL MODEL IMPORT: ${imp.fileName}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = imp.error ?: imp.statusText,
                                        fontSize = 10.sp,
                                        color = if (imp.isSuccess) NeonEmerald else if (imp.error != null) NeonCrimson else NeonCyan
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.dismissImportStatus() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = "Dismiss", tint = TextMuted, modifier = Modifier.size(18.dp))
                            }
                        }

                        if (imp.isImporting) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { imp.progressPercent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = NeonViolet,
                                trackColor = CyberBorder
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${imp.copiedMb} MB / ${if (imp.totalMb > 0) "${imp.totalMb} MB" else "..."} (${(imp.progressPercent * 100).toInt()}%)",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Auto-Scan Storage Notification
        if (scanStatusMessage != null) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = CyberSurfaceElevated,
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Text(
                                text = scanStatusMessage!!,
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.dismissScanStatus() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = "Dismiss", tint = TextMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // DIRECT ONLINE MODEL DOWNLOAD SECTION HEADER
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(17.dp))
                    Text(
                        text = "ONLINE MODEL HUB (ডাউনলোড)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonCyan,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showCustomUrlCard = !showCustomUrlCard },
                    color = CyberSurfaceElevated,
                    border = BorderStroke(0.8.dp, GlassBorderCyan)
                ) {
                    Text(
                        text = if (showCustomUrlCard) "Hide URL" else "+ Custom URL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Custom URL Direct Download Card
        if (showCustomUrlCard) {
            item {
                VibeGlassCard(
                    borderColor = GlassBorderViolet,
                    backgroundColor = GlassSurfaceElevated
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "DIRECT GGUF URL DOWNLOADER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonVioletLight
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Paste any direct Hugging Face or HTTP .gguf link to download directly to your device:",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = customUrlInput,
                            onValueChange = { customUrlInput = it },
                            placeholder = { Text("https://huggingface.co/.../model.gguf", color = TextMuted, fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 11.sp, color = TextPrimary, fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonViolet,
                                unfocusedBorderColor = CyberBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = customNameInput,
                            onValueChange = { customNameInput = it },
                            placeholder = { Text("Model Display Name (optional)", color = TextMuted, fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 11.sp, color = TextPrimary),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonViolet,
                                unfocusedBorderColor = CyberBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (customUrlInput.isNotBlank()) {
                                    viewModel.downloadModelFromUrl(customUrlInput, customNameInput)
                                    customUrlInput = ""
                                    customNameInput = ""
                                    showCustomUrlCard = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Direct Online Download", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // Curated 1-Click Online Models
        item {
            Text(
                text = "FEATURED LIGHTWEIGHT MOBILE MODELS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 0.8.sp
            )
        }

        items(ModelDownloader.CURATED_MODELS, key = { it.id }) { preset ->
            val isInstalled = models.any { it.name.contains(preset.name, ignoreCase = true) || it.filePath?.contains(preset.id) == true }
            val isCurrentlyDownloading = downloadProgress.modelId == preset.id && (downloadProgress.status == DownloadStatus.DOWNLOADING || downloadProgress.status == DownloadStatus.CONNECTING)

            VibeGlassCard(
                borderColor = if (isInstalled) GlassBorderEmerald.copy(alpha = 0.6f) else GlassBorderCyan.copy(alpha = 0.4f),
                backgroundColor = GlassSurfaceElevated
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
                            Text(
                                text = preset.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (preset.isRecommended) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NeonEmerald.copy(alpha = 0.2f))
                                        .border(0.8.dp, NeonEmerald, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "MOBILE PICK",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = NeonEmerald,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = preset.description,
                            fontSize = 10.sp,
                            color = TextSecondary,
                            lineHeight = 14.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "${preset.parameterSize} • ${preset.quantization}",
                                fontSize = 10.sp,
                                color = NeonCyan,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "~${preset.approxSizeMb} MB",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonAmber,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    if (isInstalled) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NeonEmerald.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, NeonEmerald)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(12.dp))
                                Text("Installed", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonEmerald)
                            }
                        }
                    } else if (isCurrentlyDownloading) {
                        Button(
                            onClick = { viewModel.cancelModelDownload() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCrimson.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, NeonCrimson),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Cancel", fontSize = 10.sp, color = NeonCrimson, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.downloadOnlineModel(preset) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                            border = BorderStroke(1.dp, NeonCyan),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // GGUF Import & Binary Inspector Section
        item {
            VibeGlassCard(
                borderColor = GlassBorderViolet,
                backgroundColor = GlassSurfaceElevated
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VibeGlowingIcon(icon = Icons.Default.FolderOpen, tint = NeonViolet, size = 32.dp, iconSize = 16.dp)
                            Text(
                                text = "LOAD MANUALLY DOWNLOADED MODEL",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonViolet,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "আপনি যদি ব্রাউজার বা অন্য কোথা থেকে .gguf মডেল ডাউনলোড করে থাকেন, তবে ফোন মেমোরি থেকে সরাসরি সিলেক্ট করুন অথবা পাথ দিন:",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Primary Action Buttons: File Picker & Auto Scan
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                filePickerLauncher.launch(arrayOf("*/*"))
                            },
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                            border = BorderStroke(1.dp, NeonViolet),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick .GGUF File", fontSize = 11.sp, color = NeonViolet, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.scanStorageForModels() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                            border = BorderStroke(1.dp, NeonCyan),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Auto-Scan", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "অথবা ম্যানুয়াল ফাইল পাথ দিয়ে চেক করুন:",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customPathInput,
                            onValueChange = { customPathInput = it },
                            placeholder = { Text("e.g. /sdcard/Download/model.gguf", color = TextMuted, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 11.sp, color = TextPrimary, fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonViolet,
                                unfocusedBorderColor = CyberBorder
                            )
                        )

                        Button(
                            onClick = {
                                val file = if (customPathInput.startsWith("/")) {
                                    File(customPathInput)
                                } else {
                                    File(viewModel.currentDirectory.value, customPathInput)
                                }
                                viewModel.validateGGUFFile(file)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                            border = BorderStroke(1.dp, NeonViolet),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Inspect", fontSize = 11.sp, color = NeonViolet, fontWeight = FontWeight.Bold)
                        }
                    }

                    validation?.let { meta ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = VoidBlack,
                            border = BorderStroke(1.dp, if (meta.isValid && meta.isCompatibleWithDevice) NeonEmerald else NeonCrimson)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                if (meta.isValid) {
                                    Text("Model Name: ${meta.modelName}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Architecture: ${meta.architecture}", fontSize = 11.sp, color = NeonCyan, fontFamily = FontFamily.Monospace)
                                    Text("Quantization: ${meta.quantization}", fontSize = 11.sp, color = NeonCyan, fontFamily = FontFamily.Monospace)
                                    Text("Context: ${meta.contextLength} | Size: ${meta.fileSizeBytes / (1024 * 1024)} MB", fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                                    Text("Estimated RAM: ~${meta.estimatedRamMb} MB (Available: ${telemetry.availableRamMb} MB)", fontSize = 11.sp, color = NeonAmber, fontFamily = FontFamily.Monospace)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = meta.compatibilityReason,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (meta.isCompatibleWithDevice) NeonEmerald else NeonCrimson
                                    )

                                    if (meta.isCompatibleWithDevice) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                val file = if (customPathInput.startsWith("/")) File(customPathInput) else File(viewModel.currentDirectory.value, customPathInput)
                                                viewModel.registerValidatedGGUF(meta, file)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("Register & Load Model into RAM", color = VoidBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    Text("Validation Failed: ${meta.errorMessage ?: "Invalid GGUF"}", fontSize = 11.sp, color = NeonCrimson)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Installed Models Catalog Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INSTALLED LOCAL MODELS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "${models.size} Ready",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }

        items(models, key = { it.id }) { model ->
            LocalModelCard(
                model = model,
                isRecommendedForDevice = model.ramEstimateMb < telemetry.availableRamMb,
                onLoad = { viewModel.loadModel(model) },
                onUnload = { viewModel.unloadActiveModel() },
                onDelete = { viewModel.deleteInstalledModel(model) },
                onChat = { viewModel.navigateTo(VibeScreen.CHAT) }
            )
        }
    }
}

@Composable
fun LocalModelCard(
    model: LocalModelEntity,
    isRecommendedForDevice: Boolean,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onDelete: () -> Unit,
    onChat: () -> Unit
) {
    val isActive = model.status == "ACTIVE"

    VibeGlassCard(
        borderColor = if (isActive) GlassBorderEmerald else GlassBorderCyan.copy(alpha = 0.35f),
        backgroundColor = if (isActive) GlassSurfaceElevated else CyberSurface
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
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
                    if (!model.filePath.isNullOrBlank()) {
                        Text(
                            text = model.filePath,
                            fontSize = 9.sp,
                            color = TextMuted.copy(alpha = 0.7f),
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                }

                if (isActive) {
                    VibeStatusPill(label = "ACTIVE IN RAM", statusColor = NeonEmerald)
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
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isActive) {
                        Button(
                            onClick = onChat,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.ChatBubble, contentDescription = null, tint = VoidBlack, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Chat", fontSize = 11.sp, color = VoidBlack, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onUnload,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceElevated),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, NeonAmber.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Unload", fontSize = 11.sp, color = NeonAmber, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onLoad,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceElevated),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Load to RAM", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Model", tint = TextMuted, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
