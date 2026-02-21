package com.anai.app

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
    var activePrompt by remember { mutableStateOf("No DNA extracted. Load video in Studio first.") }
    val promptHistory = remember { mutableStateListOf<String>() }

    // --- VIBE INJECTOR STATE ---
    val recentVibes by dao.getRecentVibes().collectAsState(initial = emptyList())
    var selectedVibe by remember { mutableStateOf<BlueprintEntity?>(null) }

    val charLimit = 300
    val isNearLimit = activePrompt.length > (charLimit * 0.9)
    val isAtLimit = activePrompt.length >= charLimit

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("VEO PROMPT LAB", style = MaterialTheme.typography.headlineSmall, color = Color(0xFFBB86FC))

        Text(
            text = if (selectedVibe != null) ">> AURA INJECTED: ${selectedVibe?.personaName}" else ">> ARCHITECT STATUS: READY",
            fontSize = 11.sp,
            color = if (selectedVibe != null) Color.Cyan else Color.Gray
        )

        Spacer(Modifier.height(16.dp))

        // --- EDITABLE MAIN PROMPT VIEWER ---
        Card(
            modifier = Modifier.fillMaxWidth().weight(0.35f),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            border = BorderStroke(1.dp, if (isAtLimit) Color.Red else Color(0xFFBB86FC).copy(alpha = 0.5f))
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                OutlinedTextField(
                    value = activePrompt,
                    onValueChange = { if (it.length <= charLimit) activePrompt = it },
                    modifier = Modifier.fillMaxSize(),
                    label = { Text("Blueprint Data (${activePrompt.length}/$charLimit)", fontSize = 10.sp) },
                    textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // --- 🧪 VIBE INJECTOR ROW ---
        if (recentVibes.isNotEmpty()) {
            Text("SELECT SUCCESSFUL AURA TO INJECT:", fontSize = 10.sp, color = Color.Gray)
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recentVibes) { vibe ->
                    FilterChip(
                        selected = selectedVibe?.id == vibe.id,
                        onClick = { selectedVibe = if (selectedVibe?.id == vibe.id) null else vibe },
                        label = { Text(vibe.personaName, fontSize = 10.sp) },
                        leadingIcon = if (selectedVibe?.id == vibe.id) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFBB86FC).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFFBB86FC)
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                scope.launch {
                    val videoUri = mediaManager.player.currentMediaItem?.localConfiguration?.uri?.toString()
                    if (videoUri != null) {
                        activePrompt = if (selectedVibe != null) {
                            geminiManager.extractPromptWithVibe(videoUri, selectedVibe!!.auraProfile)
                        } else {
                            geminiManager.extractPrompt(videoUri)
                        }
                    } else {
                        Toast.makeText(context, "Studio video missing!", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isProcessing,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC))
        ) {
            if (isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Icon(if (selectedVibe != null) Icons.Default.ThumbUp else Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text(if (selectedVibe != null) "EXTRACT WITH AURA" else "EXTRACT VEO DNA")
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            // FIXED: Using AutoMirrored version of List
            Icon(Icons.AutoMirrored.Filled.List, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
            Spacer(Modifier.width(8.dp))
            Text("RECALL HISTORY", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f))

        LazyColumn(modifier = Modifier.weight(0.5f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(promptHistory) { historyItem ->
                ListItem(
                    modifier = Modifier.border(1.dp, Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).clickable { activePrompt = historyItem },
                    headlineContent = { Text(text = historyItem, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, color = Color.LightGray) },
                    trailingContent = {
                        IconButton(onClick = { promptHistory.remove(historyItem) }) {
                            Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }
}