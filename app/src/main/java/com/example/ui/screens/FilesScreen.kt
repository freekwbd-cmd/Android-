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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.files.FileCategory
import com.example.core.files.SafeFileItem
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
import com.example.ui.viewmodel.VibeAIViewModel
import com.example.ui.viewmodel.VibeScreen
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun FilesScreen(
    viewModel: VibeAIViewModel,
    modifier: Modifier = Modifier
) {
    val currentDir by viewModel.currentDirectory.collectAsState()
    val files by viewModel.directoryFiles.collectAsState()
    val scope = rememberCoroutineScope()

    var showNewFileDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Dialog for new file
    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            containerColor = CyberSurfaceElevated,
            title = { Text("Create New File", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    placeholder = { Text("e.g. script.py or notes.txt", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, focusedTextColor = TextPrimary)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileName.isNotBlank()) {
                            viewModel.createNewFile(newFileName)
                            newFileName = ""
                            showNewFileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) { Text("Create", color = DarkCanvas) }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) { Text("Cancel", color = TextMuted) }
            }
        )
    }

    // Dialog for new folder
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            containerColor = CyberSurfaceElevated,
            title = { Text("Create Directory", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    placeholder = { Text("e.g. backend_scripts", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonEmerald, focusedTextColor = TextPrimary)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.createFolder(newFolderName)
                            newFolderName = ""
                            showNewFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald)
                ) { Text("Create", color = DarkCanvas) }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel", color = TextMuted) }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Path Header & Actions
        item {
            VibeCyberCard(
                borderColor = NeonEmerald.copy(alpha = 0.5f),
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
                        Column {
                            Text(
                                text = "WORKSPACE FILE ENGINE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "~/${currentDir.name}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeonCyan,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        IconButton(onClick = { viewModel.refreshFiles() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NeonEmerald)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action buttons: New File, New Folder, Create ZIP
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showNewFileDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f).testTag("new_file_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = NeonCyan)
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("New File", fontSize = 11.sp, color = NeonCyan)
                        }

                        Button(
                            onClick = { showNewFolderDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(14.dp), tint = NeonEmerald)
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Folder", fontSize = 11.sp, color = NeonEmerald)
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    val filesToZip = files.map { it.file }.filter { it.isFile }
                                    val dest = File(currentDir, "archive_${System.currentTimeMillis()}.zip")
                                    val res = SafeFileManager.createZipArchive(filesToZip, dest)
                                    statusMessage = if (res.isSuccess) "Created ${dest.name}" else "ZIP Error: ${res.exceptionOrNull()?.message}"
                                    viewModel.refreshFiles()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonAmber.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(14.dp), tint = NeonAmber)
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Zip", fontSize = 11.sp, color = NeonAmber)
                        }
                    }

                    statusMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = msg,
                            fontSize = 11.sp,
                            color = NeonEmerald,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // File List
        if (files.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = CyberSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Workspace directory is empty",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap '+ New File' above to create documents or code.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(files, key = { it.path }) { item ->
                FileItemCard(
                    item = item,
                    onOpenInCode = { viewModel.openFileInCodeWorkspace(item.file) },
                    onAnalyze = {
                        val prompt = "Perform comprehensive analysis of file ${item.name}:\n\n- Explain structure\n- Identify performance bottlenecks or bugs\n- Suggest clean architecture improvements"
                        viewModel.createNewConversation("Analyze: ${item.name}")
                        viewModel.sendMessage(prompt, item.file)
                    },
                    onExtract = {
                        scope.launch {
                            val out = File(item.file.parentFile, item.file.nameWithoutExtension + "_extracted")
                            val res = SafeFileManager.extractZipSafely(item.file, out)
                            statusMessage = if (res.isSuccess) "Extracted ${res.getOrNull()} files safely!" else "Extraction Error: ${res.exceptionOrNull()?.message}"
                            viewModel.refreshFiles()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun FileItemCard(
    item: SafeFileItem,
    onOpenInCode: () -> Unit,
    onAnalyze: () -> Unit,
    onExtract: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CyberSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                val icon = when (item.category) {
                    FileCategory.CODE -> Icons.Default.Code
                    FileCategory.DOCUMENTS -> Icons.Default.Description
                    FileCategory.IMAGES -> Icons.Default.Image
                    FileCategory.AUDIO -> Icons.Default.MusicNote
                    FileCategory.VIDEO -> Icons.Default.Videocam
                    FileCategory.ARCHIVES -> Icons.Default.FolderZip
                    FileCategory.OTHER -> if (item.isDirectory) Icons.Default.Folder else Icons.Default.Description
                }

                val iconColor = when (item.category) {
                    FileCategory.CODE -> NeonCyan
                    FileCategory.DOCUMENTS -> NeonEmerald
                    FileCategory.IMAGES -> NeonViolet
                    FileCategory.ARCHIVES -> NeonAmber
                    else -> TextSecondary
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
                }

                Column {
                    Text(
                        text = item.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = if (item.isDirectory) "Directory" else "${item.sizeBytes} bytes • ${item.category.name}",
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Quick actions
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (item.category == FileCategory.CODE || item.category == FileCategory.DOCUMENTS) {
                    IconButton(onClick = onOpenInCode, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Code, contentDescription = "Edit Code", tint = NeonCyan, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onAnalyze, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Psychology, contentDescription = "AI Analyze", tint = NeonViolet, modifier = Modifier.size(16.dp))
                    }
                } else if (item.category == FileCategory.ARCHIVES) {
                    Button(
                        onClick = onExtract,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonAmber.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Extract", fontSize = 10.sp, color = NeonAmber, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
