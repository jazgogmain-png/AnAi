package com.anai.app

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.anai.app.database.ArchitectDao
import com.anai.app.database.PersonaEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    geminiManager: GeminiManager,
    mediaManager: MediaManager,
    dao: ArchitectDao
) {
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    var showPersonaSheet by remember { mutableStateOf(false) }

    // --- SHARED STATE ---
    var videoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var contextInfo by rememberSaveable { mutableStateOf("") }
    var selectedPlatform by rememberSaveable { mutableStateOf("TikTok") }
    var selectedPersona by remember { mutableStateOf<PersonaEntity?>(null) }
    val personas by dao.getAllPersonas().collectAsState(initial = emptyList())

    // --- DRAFT BOARD STATE ---
    val captionOptions = remember { mutableStateListOf("", "", "") }
    var activeCaptionIndex by rememberSaveable { mutableIntStateOf(0) }
    var overlayText by rememberSaveable { mutableStateOf("") }
    var musicTip by rememberSaveable { mutableStateOf("") }

    val logs by geminiManager.uiLog.collectAsState()

    // --- THE DATA CATCHER ---
    LaunchedEffect(logs) {
        if (logs.contains("###ARCHITECT_DRAFT###")) {
            val draftPart = logs.substringAfter("###ARCHITECT_DRAFT###").substringBefore("###END###")
            val lines = draftPart.trim().lines()
            var currentHashtags = ""

            // First pass to find hashtags
            lines.forEach { if (it.startsWith("HT:")) currentHashtags = it.removePrefix("HT:").trim() }

            // Second pass to fill captions and attach hashtags
            lines.forEach { line ->
                when {
                    line.startsWith("C1:") -> captionOptions[0] = "${line.removePrefix("C1:").trim()}\n\n$currentHashtags"
                    line.startsWith("C2:") -> captionOptions[1] = "${line.removePrefix("C2:").trim()}\n\n$currentHashtags"
                    line.startsWith("C3:") -> captionOptions[2] = "${line.removePrefix("C3:").trim()}\n\n$currentHashtags"
                    line.startsWith("OV:") -> overlayText = line.removePrefix("OV:").trim()
                    line.startsWith("MU:") -> musicTip = line.removePrefix("MU:").trim()
                }
            }
        }
    }

    val videoUri = videoUriString?.let { Uri.parse(it) }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { videoUriString = it.toString(); mediaManager.loadVideo(it) }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Spacer(Modifier.height(8.dp)) }

        // 1. MEDIA PREVIEW & RESET
        item {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Architect Workspace", style = MaterialTheme.typography.labelLarge)
                    TextButton(onClick = {
                        videoUriString = null
                        contextInfo = ""
                        captionOptions[0] = ""; captionOptions[1] = ""; captionOptions[2] = ""
                        overlayText = ""; musicTip = ""
                    }) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset")
                    }
                }
                Card(modifier = Modifier.fillMaxWidth().height(180.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (videoUri != null) AndroidView(factory = { ctx -> PlayerView(ctx).apply { player = mediaManager.player } }, modifier = Modifier.fillMaxSize())
                        else Button(onClick = { videoPicker.launch("video/*") }) { Icon(Icons.Default.PlayArrow, null); Text("Pick Video") }
                    }
                }
            }
        }

        // 2. PERSONA & PLATFORM
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                OutlinedCard(onClick = { showPersonaSheet = true }, modifier = Modifier.weight(0.4f)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(selectedPersona?.name?.take(8) ?: "Persona", maxLines = 1, fontSize = 11.sp)
                    }
                }
                Row(modifier = Modifier.weight(0.6f), horizontalArrangement = Arrangement.End) {
                    listOf("TikTok", "YouTube", "Instagram").forEach { platform ->
                        FilterChip(
                            selected = selectedPlatform == platform,
                            onClick = { selectedPlatform = platform },
                            label = { Text(platform, fontSize = 10.sp) },
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }

        // 3. CAPTION STATION
        item {
            Column(modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Caption Station", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.weight(1f))
                    listOf("SHK", "REL", "CHS").forEachIndexed { index, label ->
                        InputChip(
                            selected = activeCaptionIndex == index,
                            onClick = { activeCaptionIndex = index },
                            label = { Text(label, fontSize = 9.sp) },
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = captionOptions[activeCaptionIndex],
                    onValueChange = { captionOptions[activeCaptionIndex] = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp).pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = {
                            if (captionOptions[activeCaptionIndex].isNotBlank()) {
                                clipboardManager.setText(AnnotatedString(captionOptions[activeCaptionIndex]))
                                Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        })
                    },
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // 4. OVERLAY & MUSIC STRIP
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = overlayText, onValueChange = { overlayText = it }, label = { Text("Overlay") }, modifier = Modifier.weight(1f).pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { clipboardManager.setText(AnnotatedString(overlayText)); Toast.makeText(context, "Overlay Copied!", Toast.LENGTH_SHORT).show() })
                })
                OutlinedTextField(value = musicTip, onValueChange = { musicTip = it }, label = { Text("Music") }, modifier = Modifier.weight(1f).pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { clipboardManager.setText(AnnotatedString(musicTip)); Toast.makeText(context, "Music Copied!", Toast.LENGTH_SHORT).show() })
                })
            }
        }

        // 5. ACTION
        item {
            Button(onClick = { scope.launch { geminiManager.analyzeVideo(contextInfo, selectedPlatform, selectedPersona?.name, selectedPersona?.instructions, videoUriString) } }, modifier = Modifier.fillMaxWidth()) {
                Text("EXECUTE VIRAL SCAN")
            }
        }

        // 6. NERD LOG
        item {
            Surface(color = Color.Black, modifier = Modifier.fillMaxWidth().height(120.dp).border(1.dp, Color.Green, MaterialTheme.shapes.small)) {
                val scrollState = rememberScrollState()
                Text(logs.substringBefore("###ARCHITECT_DRAFT###"), color = Color.Green, fontFamily = FontFamily.Monospace, fontSize = 10.sp, modifier = Modifier.padding(8.dp).verticalScroll(scrollState))
            }
        }
    }

    if (showPersonaSheet) {
        ModalBottomSheet(onDismissRequest = { showPersonaSheet = false }) {
            LazyColumn(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                items(personas) { persona ->
                    ListItem(headlineContent = { Text(persona.name) }, modifier = Modifier.clickable { selectedPersona = persona; showPersonaSheet = false })
                }
            }
        }
    }
}