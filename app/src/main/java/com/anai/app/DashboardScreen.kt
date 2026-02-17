package com.anai.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State for UI inputs
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var contextInfo by remember { mutableStateOf("") }
    var selectedPlatform by remember { mutableStateOf("TikTok") }

    // Collect the Nerd Log from the manager
    val logs by geminiManager.uiLog.collectAsState()

    // Video Picker Launcher
    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            videoUri = it
            mediaManager.loadVideo(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AnAi Video Architect", style = MaterialTheme.typography.headlineSmall) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // 1. VIDEO PREVIEW SECTION
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    if (videoUri != null) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = mediaManager.player
                                    useController = true
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Button(onClick = { pickerLauncher.launch("video/*") }) {
                                Text("Select Video (4K/1080p)")
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
                    label = { Text("Context (e.g., Drifting Grandma)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Tell Gemini what's happening...") }
                )
            }

            // 3. PLATFORM SELECTION
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                            geminiManager.analyzeVideo(
                                contextInfo = contextInfo,
                                platform = selectedPlatform,
                                videoUriString = videoUri?.toString()
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = videoUri != null,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("START G3 ANALYSIS")
                }
            }

            // 5. THE NERD LOG (BIG EYES VERSION)
            item {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "SYSTEM STATUS",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Surface(
                        color = Color(0xFF0A0A0A), // Deepest black
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .border(1.dp, Color(0xFF00FF00), MaterialTheme.shapes.medium),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        val scrollState = rememberScrollState()

                        // Auto-scroll to bottom when new logs arrive
                        LaunchedEffect(logs) {
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }

                        Text(
                            text = logs,
                            modifier = Modifier
                                .padding(12.dp)
                                .verticalScroll(scrollState),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 20.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color(0xFF00FF00), // High-vis Matrix Green
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}