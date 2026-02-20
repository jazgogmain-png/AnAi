package com.anai.app

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView

@Composable
fun ArchitectHeader(
    geminiManager: GeminiManager,
    mediaManager: MediaManager,
    videoUriString: String?
) {
    val logs by geminiManager.uiLog.collectAsState()
    val isProcessing by geminiManager.isProcessing.collectAsState()

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))

        // 1. THE MATRIX
        Surface(
            color = Color.Black,
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .border(1.dp, Color.Green.copy(alpha = 0.5f), MaterialTheme.shapes.small)
        ) {
            val scrollState = rememberScrollState()
            LaunchedEffect(logs) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
            Text(
                text = logs.substringBefore("###ARCHITECT_DRAFT###"),
                color = Color.Green,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                modifier = Modifier.padding(8.dp).verticalScroll(scrollState)
            )
        }

        if (isProcessing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = Color.Green,
                trackColor = Color.Black
            )
        }

        Spacer(Modifier.height(12.dp))

        // 2. VIDEO PREVIEW
        Card(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (videoUriString != null) {
                    AndroidView(factory = { ctx ->
                        PlayerView(ctx).apply { player = mediaManager.player }
                    }, modifier = Modifier.fillMaxSize())
                } else {
                    Text("No Video Selected", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}