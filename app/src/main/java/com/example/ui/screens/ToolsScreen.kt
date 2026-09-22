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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Videocam
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
import com.example.core.files.SafeFileManager
import com.example.ui.components.VibeCyberCard
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
import java.io.File

@Composable
fun ToolsScreen(
    viewModel: VibeAIViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val files by viewModel.directoryFiles.collectAsState()

    var encPassphrase by remember { mutableStateOf("") }
    var encStatus by remember { mutableStateOf<String?>(null) }
    var mediaAnalysisResult by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: AES-256 Cryptographic Tools
        item {
            VibeCyberCard(
                borderColor = NeonViolet.copy(alpha = 0.5f),
                backgroundColor = CyberSurfaceElevated
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(20.dp))
                        Text(
                            text = "AES-256 FILE CIPHER WORKSTATION",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Hardware-grade AES/CBC/PKCS5Padding encryption with PBKDF2 key derivation and random 128-bit salt/IV.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = encPassphrase,
                        onValueChange = { encPassphrase = it },
                        placeholder = { Text("Enter secret passphrase...", color = TextMuted, fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("crypto_passphrase_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonViolet,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (encPassphrase.isBlank()) {
                                    Toast.makeText(context, "Please enter a passphrase", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                scope.launch {
                                    val target = files.firstOrNull { it.file.isFile && !it.name.endsWith(".enc") }?.file
                                    if (target != null) {
                                        val dest = File(target.parentFile, target.name + ".enc")
                                        val res = com.example.core.security.CryptoManager.encryptFileGcm(target, dest, encPassphrase.toCharArray())
                                        encStatus = if (res.isSuccess) "✓ AES-GCM Encrypted ${target.name} -> ${dest.name}" else "Encryption failed: ${res.exceptionOrNull()?.message}"
                                        viewModel.refreshFiles()
                                    } else {
                                        encStatus = "No unencrypted file found in workspace."
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonViolet),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = NeonViolet)
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Encrypt (AES-GCM)", fontSize = 11.sp, color = NeonViolet)
                        }

                        Button(
                            onClick = {
                                if (encPassphrase.isBlank()) {
                                    Toast.makeText(context, "Please enter a passphrase", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                scope.launch {
                                    val target = files.firstOrNull { it.name.endsWith(".enc") }?.file
                                    if (target != null) {
                                        val dest = File(target.parentFile, target.name.removeSuffix(".enc") + "_decrypted.txt")
                                        val res = com.example.core.security.CryptoManager.decryptFileGcm(target, dest, encPassphrase.toCharArray())
                                        encStatus = if (res.isSuccess) "✓ Authenticated & Decrypted -> ${dest.name}" else "Decryption failed: ${res.exceptionOrNull()?.message ?: "Bad passphrase or corrupted file"}"
                                        viewModel.refreshFiles()
                                    } else {
                                        encStatus = "No .enc encrypted file found in workspace."
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(14.dp), tint = NeonCyan)
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Decrypt (AES-GCM)", fontSize = 11.sp, color = NeonCyan)
                        }
                    }

                    encStatus?.let { status ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = status, fontSize = 11.sp, color = NeonCyan, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // Section: Multimedia AI Tool Suite
        item {
            VibeCyberCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "MULTIMODAL AI INSPECTION SUITE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MediaActionRow(
                            title = "Image Vision & OCR Inspection",
                            subtitle = "Real dimension, MIME & vision limit check",
                            icon = Icons.Default.Image,
                            color = NeonCyan,
                            onClick = {
                                scope.launch {
                                    val img = files.firstOrNull { it.category == com.example.core.files.FileCategory.IMAGES }?.file
                                    if (img != null) {
                                        val inspect = com.example.core.files.DocumentParser.inspectImageSafely(img)
                                        mediaAnalysisResult = inspect.getOrNull()?.notice ?: "Failed to inspect ${img.name}"
                                    } else {
                                        mediaAnalysisResult = "No image file in workspace.\nLIMITATION: Local model is text-only. Semantic image comprehension and OCR text extraction require an online vision model (e.g. GPT-4o) or on-device LLaVA weights."
                                    }
                                }
                            }
                        )

                        MediaActionRow(
                            title = "Audio Waveform & Transcription",
                            subtitle = "Real duration, audio metadata & STT status",
                            icon = Icons.Default.MusicNote,
                            color = NeonEmerald,
                            onClick = {
                                scope.launch {
                                    val audio = files.firstOrNull { it.category == com.example.core.files.FileCategory.AUDIO }?.file
                                    if (audio != null) {
                                        val inspect = com.example.core.files.DocumentParser.inspectAudioSafely(audio)
                                        mediaAnalysisResult = inspect.getOrNull()?.notice ?: "Failed to inspect ${audio.name}"
                                    } else {
                                        mediaAnalysisResult = "No audio file in workspace.\nLIMITATION: On-device Speech-To-Text (STT) requires an installed Whisper engine. Connect an online API or install Whisper weights."
                                    }
                                }
                            }
                        )

                        MediaActionRow(
                            title = "Video Keyframe Sampler",
                            subtitle = "Real resolution, duration & frame bounds",
                            icon = Icons.Default.Videocam,
                            color = NeonCrimson,
                            onClick = {
                                scope.launch {
                                    val video = files.firstOrNull { it.category == com.example.core.files.FileCategory.VIDEO }?.file
                                    if (video != null) {
                                        val inspect = com.example.core.files.DocumentParser.inspectVideoSafely(video)
                                        mediaAnalysisResult = inspect.getOrNull()?.notice ?: "Failed to inspect ${video.name}"
                                    } else {
                                        mediaAnalysisResult = "No video file in workspace.\nLIMITATION: Video understanding requires an online multimodal model or native vision model."
                                    }
                                }
                            }
                        )
                    }

                    mediaAnalysisResult?.let { result ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = VoidBlack,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
                        ) {
                            Text(
                                text = result,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MediaActionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        color = CyberSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = subtitle, fontSize = 10.sp, color = TextMuted)
            }

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("Inspect", fontSize = 10.sp, color = color)
            }
        }
    }
}
