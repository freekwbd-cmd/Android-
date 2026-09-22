package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack

data class PendingCodeModification(
    val fileName: String,
    val originalCode: String,
    val proposedCode: String,
    val onApply: () -> Unit,
    val onReject: () -> Unit
)

@Composable
fun CodeDiffModal(
    modification: PendingCodeModification,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurfaceElevated,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.CompareArrows, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                Text(
                    text = "File Modification Checkpoint: ${modification.fileName}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Review changes before writing to local file storage:",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                // Original Code Box
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ORIGINAL VERSION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCrimson,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        color = VoidBlack,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCrimson.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = modification.originalCode.ifEmpty { "(Empty file)" },
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Proposed Code Box
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PROPOSED AI MODIFICATION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonEmerald,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        color = VoidBlack,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = modification.proposedCode,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NeonCyan,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    modification.onApply()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = VoidBlack)
                Spacer(modifier = Modifier.size(4.dp))
                Text("Apply Changes", color = VoidBlack, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    modification.onReject()
                    onDismiss()
                },
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCrimson)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = NeonCrimson)
                Spacer(modifier = Modifier.size(4.dp))
                Text("Reject", color = NeonCrimson)
            }
        }
    )
}
