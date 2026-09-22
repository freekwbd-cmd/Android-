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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.router.AIMode
import com.example.data.model.ChatMessageEntity
import com.example.ui.components.VibeCodeBlock
import com.example.ui.components.VibeCyberCard
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack
import com.example.ui.viewmodel.VibeAIViewModel
import java.io.File

@Composable
fun ChatScreen(
    viewModel: VibeAIViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.currentMessages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val streamingText by viewModel.streamingMessageText.collectAsState()
    val mode by viewModel.router.currentMode.collectAsState()
    val listState = rememberLazyListState()

    var inputPrompt by remember { mutableStateOf("") }
    var attachedFile by remember { mutableStateOf<File?>(null) }
    val files by viewModel.directoryFiles.collectAsState()
    var showAttachmentPicker by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size, streamingText) {
        if (messages.isNotEmpty() || streamingText.isNotEmpty()) {
            listState.animateScrollToItem((messages.size).coerceAtLeast(0))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
    ) {
        // Chat Header with conversation details & New Chat button
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CyberSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (mode == AIMode.OFFLINE) "VibeAI • Offline Engine" else "VibeAI • Online Provider",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (mode == AIMode.OFFLINE) NeonEmerald else NeonCyan
                    )
                    Text(
                        text = viewModel.router.localEngine.activeModelName,
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.createNewConversation() }
                            .testTag("new_chat_button"),
                        color = CyberSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCrimson.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Chat",
                                tint = NeonCrimson,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "New Session",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeonCrimson
                            )
                        }
                    }
                }
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatMessageItem(message = msg)
            }

            // Live streaming message placeholder
            if (isGenerating && streamingText.isNotEmpty()) {
                item {
                    StreamingMessageBubble(
                        content = streamingText,
                        modelName = viewModel.router.localEngine.activeModelName
                    )
                }
            } else if (isGenerating) {
                item {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = NeonCrimson,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Synthesizing neural tokens...",
                            fontSize = 12.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Attachment selection preview
        attachedFile?.let { file ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                color = CyberSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Attached: ${file.name} (${file.length()} bytes)",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = NeonCyan
                    )
                    IconButton(
                        onClick = { attachedFile = null },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove attachment",
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Attachment file quick selector popup
        if (showAttachmentPicker) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = CyberSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "SELECT WORKSPACE FILE TO ATTACH",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    files.take(4).forEach { f ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    attachedFile = f.file
                                    showAttachmentPicker = false
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = f.name,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = f.category.name,
                                fontSize = 10.sp,
                                color = NeonCyan
                            )
                        }
                    }
                }
            }
        }

        // Bottom Input Row
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CyberSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Attach button
                IconButton(
                    onClick = { showAttachmentPicker = !showAttachmentPicker },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Attach File",
                        tint = if (attachedFile != null) NeonCyan else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Input TextField
                OutlinedTextField(
                    value = inputPrompt,
                    onValueChange = { inputPrompt = it },
                    placeholder = {
                        Text(
                            text = "Ask VibeAI or prompt code...",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCrimson,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = NeonCrimson
                    ),
                    maxLines = 4
                )

                // Send or Stop button
                if (isGenerating) {
                    IconButton(
                        onClick = { viewModel.stopGeneration() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(NeonCrimson)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (inputPrompt.isNotBlank() || attachedFile != null) {
                                viewModel.sendMessage(inputPrompt, attachedFile)
                                inputPrompt = ""
                                attachedFile = null
                                showAttachmentPicker = false
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (inputPrompt.isNotBlank() || attachedFile != null) NeonCrimson else CyberBorder)
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessageEntity) {
    val isUser = message.role == "user"
    val clipboard: ClipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (isUser) {
            Surface(
                shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
                color = CyberSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCrimson.copy(alpha = 0.5f))
            ) {
                Text(
                    text = message.content,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        } else {
            // Assistant glass panel
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp),
                color = CyberSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header with model badge & copy button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(NeonCrimson)
                            )
                            Text(
                                text = message.modelName.ifEmpty { "VibeAI Neural Core" },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCrimson,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        IconButton(
                            onClick = { clipboard.setText(AnnotatedString(message.content)) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy message",
                                tint = TextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    RenderMarkdownText(message.content)

                    // Generation Telemetry Footer
                    if (message.latencyMs > 0 || message.tokenCount > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${message.tokenCount} tokens in ${message.latencyMs}ms",
                                fontSize = 10.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "•",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                            Text(
                                text = "${if (message.isOffline) "100% Offline" else "Online API"}",
                                fontSize = 10.sp,
                                color = if (message.isOffline) NeonEmerald else NeonCyan,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreamingMessageBubble(content: String, modelName: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp),
        color = CyberSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCrimson.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(NeonCrimson)
                )
                Text(
                    text = "$modelName (Streaming)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCrimson,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            RenderMarkdownText(content)
        }
    }
}

@Composable
fun RenderMarkdownText(text: String) {
    val codeBlockRegex = Regex("```(\\w*)\\n([\\s\\S]*?)```")
    val matches = codeBlockRegex.findAll(text).toList()

    if (matches.isEmpty()) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = TextPrimary,
            lineHeight = 19.sp
        )
    } else {
        var lastIndex = 0
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (match in matches) {
                val nonCodePart = text.substring(lastIndex, match.range.first).trim()
                if (nonCodePart.isNotEmpty()) {
                    Text(
                        text = nonCodePart,
                        fontSize = 13.sp,
                        color = TextPrimary,
                        lineHeight = 19.sp
                    )
                }

                val lang = match.groupValues[1].ifEmpty { "code" }
                val code = match.groupValues[2]
                VibeCodeBlock(code = code, language = lang)

                lastIndex = match.range.last + 1
            }

            val remaining = text.substring(lastIndex).trim()
            if (remaining.isNotEmpty()) {
                Text(
                    text = remaining,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 19.sp
                )
            }
        }
    }
}
