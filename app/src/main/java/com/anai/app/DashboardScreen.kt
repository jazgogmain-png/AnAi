package com.anai.app

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.anai.app.database.ArchitectDao
import com.anai.app.database.EngineEntity
import com.anai.app.database.PersonaEntity
import com.anai.app.database.PlatformEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    geminiManager: GeminiManager,
    mediaManager: MediaManager,
    dao: ArchitectDao,
    selectedPlatform: PlatformEntity? = null
) {
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var showPersonaSheet by remember { mutableStateOf(false) }

    val personas by dao.getAllPersonas().collectAsState(initial = emptyList())
    val engines by dao.getAllEngines().collectAsState(initial = emptyList())

    // --- UI STATE ---
    var videoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPersona by remember { mutableStateOf<PersonaEntity?>(null) }
    var selectedEngine by remember { mutableStateOf<EngineEntity?>(null) }
    var activeCaptionIndex by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(selectedPlatform, engines, personas) {
        if (selectedPlatform != null) {
            selectedEngine = engines.find { it.name.contains(selectedPlatform.name, ignoreCase = true) }
                ?: engines.firstOrNull()
            if (selectedPersona == null) selectedPersona = personas.firstOrNull()
        }
    }

    val isProcessing by geminiManager.isProcessing.collectAsState()
    val captionOptions by geminiManager.captionOptions.collectAsState()
    val overlayText by geminiManager.overlayText.collectAsState()
    val musicTip by geminiManager.musicTip.collectAsState()
    val seoTags by geminiManager.seoTags.collectAsState()
    val descPart by geminiManager.descPart.collectAsState()
    val hashtagPart by geminiManager.hashtagPart.collectAsState()

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { videoUriString = it.toString(); mediaManager.loadVideo(it); geminiManager.clearLog() }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ArchitectHeader(geminiManager, mediaManager, videoUriString) }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = if (selectedPlatform == null) "Master Studio" else "${selectedPlatform.name} Studio", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                Row {
                    if (videoUriString == null) {
                        IconButton(onClick = { videoPicker.launch("video/*") }) {
                            Icon(Icons.Default.Add, null, tint = Color.Cyan)
                        }
                    }
                    IconButton(onClick = { videoUriString = null; geminiManager.clearLog() }) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedCard(onClick = { showPersonaSheet = true }, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = Color.Cyan)
                        Spacer(Modifier.width(8.dp))
                        Text(selectedPersona?.name ?: "Select Soul", fontSize = 12.sp)
                    }
                }
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    engines.forEach { engine ->
                        FilterChip(selected = selectedEngine?.id == engine.id, onClick = { selectedEngine = engine }, label = { Text(engine.name, fontSize = 10.sp) })
                    }
                }
            }
        }

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
                    value = currentCap, onValueChange = {}, readOnly = true,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp).pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { if (currentCap.isNotBlank()) { clipboardManager.setText(AnnotatedString(currentCap)); Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show() } })
                    },
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // --- DUMB DESCRIPTION BOX (Strictly what G3 sends) ---
        if (descPart.isNotBlank()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().border(1.dp, Color.Cyan.copy(alpha = 0.5f), MaterialTheme.shapes.medium).padding(12.dp)) {
                    Text("Architect Description", style = MaterialTheme.typography.labelLarge, color = Color.Cyan)
                    OutlinedTextField(value = descPart, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp).pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { clipboardManager.setText(AnnotatedString(descPart)); Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show() })
                    })
                }
            }
        }

        if (hashtagPart.isNotBlank()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().border(1.dp, Color.Magenta.copy(alpha = 0.5f), MaterialTheme.shapes.medium).padding(12.dp)) {
                    Text("Viral Hashtags", style = MaterialTheme.typography.labelLarge, color = Color.Magenta)
                    OutlinedTextField(value = hashtagPart, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { clipboardManager.setText(AnnotatedString(hashtagPart)); Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show() })
                    })
                }
            }
        }

        item {
            Button(
                onClick = {
                    scope.launch {
                        geminiManager.analyzeVideo("", selectedPlatform?.name ?: "Master", selectedPersona?.name, selectedPersona?.instructions, videoUriString, selectedEngine?.instructions, selectedEngine?.draftTemplate)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = selectedEngine != null && selectedPersona != null && videoUriString != null && !isProcessing
            ) {
                if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Green)
                else Text("EXECUTE BLUEPRINT SCAN", fontWeight = FontWeight.Bold)
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