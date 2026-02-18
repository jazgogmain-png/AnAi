package com.anai.app

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.anai.app.database.ArchitectDao
import com.anai.app.database.EngineEntity
import com.anai.app.database.PersonaEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    geminiManager: GeminiManager,
    mediaManager: MediaManager,
    dao: ArchitectDao
) {
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var showPersonaSheet by remember { mutableStateOf(false) }

    // --- DATABASE STATE ---
    val personas by dao.getAllPersonas().collectAsState(initial = emptyList())
    val engines by dao.getAllEngines().collectAsState(initial = emptyList())

    // --- UI STATE ---
    var videoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var contextInfo by rememberSaveable { mutableStateOf("") }
    var selectedPersona by remember { mutableStateOf<PersonaEntity?>(null) }
    var selectedEngine by remember { mutableStateOf<EngineEntity?>(null) }
    var activeCaptionIndex by rememberSaveable { mutableIntStateOf(0) }

    // --- OBSERVE PERSISTENT DATA FROM MANAGER ---
    val logs by geminiManager.uiLog.collectAsState()
    val captionOptions by geminiManager.captionOptions.collectAsState()
    val overlayText by geminiManager.overlayText.collectAsState()
    val musicTip by geminiManager.musicTip.collectAsState()
    val seoTags by geminiManager.seoTags.collectAsState()

    val videoUri = videoUriString?.let { Uri.parse(it) }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { videoUriString = it.toString(); mediaManager.loadVideo(it); geminiManager.clearLog() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        // 1. THE MATRIX (Now at the top)
        item {
            Surface(
                color = Color.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .border(1.dp, Color.Green.copy(alpha = 0.5f), MaterialTheme.shapes.small)
            ) {
                val scrollState = rememberScrollState()
                // Auto-scroll to bottom of logs
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
        }

        // 2. MEDIA PREVIEW & RESET
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Architect Workspace", style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = {
                    videoUriString = null
                    contextInfo = ""
                    geminiManager.clearLog()
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

        // 3. BLUEPRINT SELECTION
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedCard(onClick = { showPersonaSheet = true }, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(selectedPersona?.name ?: "Select Persona Soul", fontSize = 12.sp)
                    }
                }

                Text("Platform Engine Blueprint:", style = MaterialTheme.typography.labelSmall)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    engines.forEach { engine ->
                        FilterChip(
                            selected = selectedEngine?.id == engine.id,
                            onClick = { selectedEngine = engine },
                            label = { Text(engine.name, fontSize = 10.sp) }
                        )
                    }
                }
            }
        }

        // 4. STRATEGY OUTPUT
        item {
            Column(modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Draft Station", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.weight(1f))
                    listOf("SHK", "REL", "CHS").forEachIndexed { index, label ->
                        InputChip(selected = activeCaptionIndex == index, onClick = { activeCaptionIndex = index }, label = { Text(label, fontSize = 9.sp) }, modifier = Modifier.padding(start = 4.dp))
                    }
                }
                val currentCap = if (captionOptions.size > activeCaptionIndex) captionOptions[activeCaptionIndex] else ""
                OutlinedTextField(
                    value = currentCap,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp).pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = {
                            if (currentCap.isNotBlank()) {
                                clipboardManager.setText(AnnotatedString(currentCap))
                                Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                            }
                        })
                    },
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // 5. METADATA & EXECUTE
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = overlayText, onValueChange = {}, label = { Text("Overlay") }, modifier = Modifier.weight(1f).pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { clipboardManager.setText(AnnotatedString(overlayText)); Toast.makeText(context, "Overlay Copied!", Toast.LENGTH_SHORT).show() })
                    })
                    OutlinedTextField(value = musicTip, onValueChange = {}, label = { Text("Music") }, modifier = Modifier.weight(1f).pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { clipboardManager.setText(AnnotatedString(musicTip)); Toast.makeText(context, "Music Copied!", Toast.LENGTH_SHORT).show() })
                    })
                }

                if (seoTags.isNotBlank()) {
                    OutlinedTextField(value = seoTags, onValueChange = {}, label = { Text("SEO Tags") }, modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { clipboardManager.setText(AnnotatedString(seoTags)); Toast.makeText(context, "Tags Copied!", Toast.LENGTH_SHORT).show() })
                    })
                }

                Button(
                    onClick = {
                        scope.launch {
                            geminiManager.analyzeVideo(
                                contextInfo = contextInfo,
                                platform = selectedEngine?.name ?: "Unknown",
                                personaName = selectedPersona?.name,
                                personaInstructions = selectedPersona?.instructions,
                                videoUriString = videoUriString,
                                engineInstructions = selectedEngine?.instructions,
                                engineTemplate = selectedEngine?.draftTemplate
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = selectedEngine != null && selectedPersona != null && videoUriString != null,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("EXECUTE BLUEPRINT SCAN", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
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