package com.anai.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    geminiManager: GeminiManager,
    mediaManager: MediaManager
) {
    val scope = rememberCoroutineScope()

    // --- STATE ---
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var statsUri by remember { mutableStateOf<Uri?>(null) }
    var contextInfo by remember { mutableStateOf("") }
    var selectedPlatform by remember { mutableStateOf("TikTok") }

    val logs by geminiManager.uiLog.collectAsState()

    // --- PICKERS ---
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            videoUri = it
            statsUri = null // Clear stats if we pick a video
            mediaManager.loadVideo(it)
        }
    }

    val statsPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            statsUri = it
            videoUri = null // Clear video if we pick a screenshot
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        // 1. MEDIA INPUT BOX
        item {
            Card(
                modifier = Modifier.fillMaxWidth().height(250.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (videoUri != null) {
                        AndroidView(
                            factory = { ctx -> PlayerView(ctx).apply { player = mediaManager.player } },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (statsUri != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(48.dp))
                            Text("Stats Screenshot Selected", style = MaterialTheme.typography.titleMedium)
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { videoPicker.launch("video/*") }) {
                                Icon(Icons.Default.PlayArrow, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Video")
                            }
                            Button(onClick = { statsPicker.launch("image/*") }) {
                                Icon(Icons.Default.Add, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Stats")
                            }
                        }
                    }
                }
            }
        }

        // 2. CONTEXT INPUT
        item {
            OutlinedTextField(
                value = contextInfo,
                onValueChange = { contextInfo = it },
                label = { Text("Context / Notes") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Describe the video or the stats data...") }
            )
        }

        // 3. PLATFORM CHIPS
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("TikTok", "YouTube", "Instagram").forEach { platform ->
                    FilterChip(
                        selected = selectedPlatform == platform,
                        onClick = { selectedPlatform = platform },
                        label = { Text(platform) }
                    )
                }
            }
        }

        // 4. ACTION BUTTON
        item {
            Button(
                onClick = {
                    scope.launch {
                        if (videoUri != null) {
                            geminiManager.analyzeVideo(contextInfo, selectedPlatform, videoUri.toString())
                        } else if (statsUri != null) {
                            geminiManager.analyzeStats(statsUri!!, contextInfo)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = (videoUri != null || statsUri != null),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(if (videoUri != null) "ARCHITECT PIXEL SCAN" else "ANALYZE PERFORMANCE DATA")
            }
        }

        // 5. THE NERD LOG
        item {
            Column {
                Text("SYSTEM STATUS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                Surface(
                    color = Color(0xFF0A0A0A),
                    modifier = Modifier.fillMaxWidth().height(220.dp).border(1.dp, Color(0xFF00FF00), MaterialTheme.shapes.medium),
                    shape = MaterialTheme.shapes.medium
                ) {
                    val scrollState = rememberScrollState()
                    LaunchedEffect(logs) { scrollState.animateScrollTo(scrollState.maxValue) }
                    Text(
                        text = logs,
                        modifier = Modifier.padding(12.dp).verticalScroll(scrollState),
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp, letterSpacing = 0.5.sp),
                        color = Color(0xFF00FF00),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}