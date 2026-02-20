package com.anai.app

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun PromptLabScreen(geminiManager: GeminiManager, mediaManager: MediaManager) {
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val isProcessing by geminiManager.isProcessing.collectAsState()
    var activePrompt by remember { mutableStateOf("No DNA extracted. Load video in Studio first.") }
    val promptHistory = remember { mutableStateListOf<String>() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("VEO PROMPT LAB", style = MaterialTheme.typography.headlineSmall, color = Color(0xFFBB86FC))
        Text("Reverse-engineer visual DNA. Edit text to add a title!", fontSize = 11.sp, color = Color.Gray)

        Spacer(Modifier.height(16.dp))

        // --- EDITABLE MAIN PROMPT VIEWER ---
        Card(
            modifier = Modifier.fillMaxWidth().weight(0.4f),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            border = BorderStroke(1.dp, Color(0xFFBB86FC).copy(alpha = 0.5f))
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                // We use OutlinedTextField here so you can tap and type
                OutlinedTextField(
                    value = activePrompt,
                    onValueChange = { activePrompt = it },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = TextStyle(
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                if (activePrompt.length > 20) {
                    SmallFloatingActionButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(activePrompt))
                            if (!promptHistory.contains(activePrompt)) {
                                promptHistory.add(0, activePrompt)
                            }
                            Toast.makeText(context, "DNA & Title Archived!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                        containerColor = Color(0xFFBB86FC)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Copy")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    val videoUri = mediaManager.player.currentMediaItem?.localConfiguration?.uri?.toString()
                    if (videoUri != null) {
                        activePrompt = geminiManager.extractPrompt(videoUri)
                    } else {
                        Toast.makeText(context, "Studio video missing!", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isProcessing,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC))
        ) {
            if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            else {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("EXTRACT VEO DNA")
            }
        }

        Spacer(Modifier.height(24.dp))

        Text("RECALL HISTORY", style = MaterialTheme.typography.labelLarge)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        LazyColumn(modifier = Modifier.weight(0.6f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(promptHistory) { historyItem ->
                ListItem(
                    modifier = Modifier
                        .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .clickable { activePrompt = historyItem },
                    headlineContent = {
                        Text(text = historyItem, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                    },
                    trailingContent = {
                        IconButton(onClick = { promptHistory.remove(historyItem) }) {
                            Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.6f))
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }
}