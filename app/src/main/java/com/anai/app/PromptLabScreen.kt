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
import androidx.compose.material.icons.automirrored.filled.List
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
import com.anai.app.database.ArchitectDao
import com.anai.app.database.BlueprintEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptLabScreen(geminiManager: GeminiManager, mediaManager: MediaManager, dao: ArchitectDao) {
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val isProcessing by geminiManager.isProcessing.collectAsState()
    var activePrompt by remember { mutableStateOf("Ready to extract cinematic DNA...") }
    val promptHistory = remember { mutableStateListOf<String>() }

    val charLimit = 600
    val isAtLimit = activePrompt.length >= charLimit

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("PROMPT LAB", style = MaterialTheme.typography.headlineSmall, color = Color.Cyan)

            // THE SAVE BUTTON
            IconButton(onClick = {
                if (activePrompt.isNotBlank()) {
                    promptHistory.add(0, activePrompt)
                    Toast.makeText(context, "Prompt Saved!", Toast.LENGTH_SHORT).show()
                }
            }) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Save", tint = Color.Green)
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().weight(0.4f),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            border = BorderStroke(1.dp, if (isAtLimit) Color.Red else Color.Cyan.copy(alpha = 0.3f))
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                OutlinedTextField(
                    value = activePrompt,
                    onValueChange = { if (it.length <= charLimit) activePrompt = it },
                    modifier = Modifier.fillMaxSize(),
                    label = { Text("Cinematic Prompt (${activePrompt.length}/$charLimit)", fontSize = 10.sp) },
                    textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                scope.launch {
                    val videoUri = mediaManager.player.currentMediaItem?.localConfiguration?.uri?.toString()
                    if (videoUri != null) {
                        activePrompt = geminiManager.extractPrompt(videoUri)
                    } else {
                        Toast.makeText(context, "Load video in Studio first!", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isProcessing
        ) {
            if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            else Text("EXTRACT CINEMATIC DNA")
        }

        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.List, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
            Spacer(Modifier.width(8.dp))
            Text("SAVED PROMPTS", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f))

        LazyColumn(modifier = Modifier.weight(0.4f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(promptHistory) { historyItem ->
                ListItem(
                    modifier = Modifier.border(1.dp, Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).clickable { activePrompt = historyItem },
                    headlineContent = { Text(text = historyItem, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                    trailingContent = {
                        IconButton(onClick = { promptHistory.remove(historyItem) }) {
                            Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.4f))
                        }
                    }
                )
            }
        }
    }
}