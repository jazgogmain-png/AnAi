package com.anai.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
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
    val sheetState = rememberModalBottomSheetState()
    var showPersonaSheet by remember { mutableStateOf(false) }

    // --- STATE ---
    var videoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var statsUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var contextInfo by rememberSaveable { mutableStateOf("") }
    var selectedPlatform by rememberSaveable { mutableStateOf("TikTok") }

    var selectedPersona by remember { mutableStateOf<PersonaEntity?>(null) }
    val personas by dao.getAllPersonas().collectAsState(initial = emptyList())

    val logs by geminiManager.uiLog.collectAsState()

    val videoUri = videoUriString?.let { Uri.parse(it) }
    val statsUri = statsUriString?.let { Uri.parse(it) }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { videoUriString = it.toString(); statsUriString = null; mediaManager.loadVideo(it) }
    }
    val statsPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { statsUriString = it.toString(); videoUriString = null }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Architect Workspace", style = MaterialTheme.typography.labelLarge)
                    TextButton(onClick = { videoUriString = null; statsUriString = null; contextInfo = ""; selectedPersona = null }) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset")
                    }
                }
                Card(modifier = Modifier.fillMaxWidth().height(220.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (videoUri != null) {
                            AndroidView(factory = { ctx -> PlayerView(ctx).apply { player = mediaManager.player } }, modifier = Modifier.fillMaxSize())
                        } else if (statsUri != null) {
                            // Swapped for Icons.Default.List (100% Core)
                            Icon(Icons.Default.List, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = { videoPicker.launch("video/*") }) { Icon(Icons.Default.PlayArrow, null); Text("Video") }
                                Button(onClick = { statsPicker.launch("image/*") }) { Icon(Icons.Default.Add, null); Text("Stats") }
                            }
                        }
                    }
                }
            }
        }

        item {
            OutlinedCard(onClick = { showPersonaSheet = true }, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Active Persona", style = MaterialTheme.typography.labelSmall)
                        Text(selectedPersona?.name ?: "Standard Analyst (Default)", style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, null)
                }
            }
        }

        item {
            OutlinedTextField(value = contextInfo, onValueChange = { contextInfo = it }, label = { Text("Architect Notes") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("TikTok", "YouTube", "Instagram").forEach { platform ->
                    FilterChip(selected = selectedPlatform == platform, onClick = { selectedPlatform = platform }, label = { Text(platform) })
                }
            }
        }

        item {
            Button(
                onClick = {
                    scope.launch {
                        if (videoUri != null) {
                            geminiManager.analyzeVideo(contextInfo, selectedPlatform, selectedPersona?.name, selectedPersona?.instructions, videoUri.toString())
                        } else if (statsUri != null) {
                            geminiManager.analyzeStats(statsUri, contextInfo)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = (videoUri != null || statsUri != null),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(if (videoUri != null) "EXECUTE PIXEL SCAN" else "EXECUTE DATA SCAN")
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("SYSTEM STATUS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                IconButton(onClick = { clipboardManager.setText(AnnotatedString(logs)) }) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(20.dp))
                }
            }
            Surface(
                color = Color(0xFF0A0A0A),
                modifier = Modifier.fillMaxWidth().height(200.dp).border(1.dp, Color(0xFF00FF00), MaterialTheme.shapes.medium),
                shape = MaterialTheme.shapes.medium
            ) {
                val scrollState = rememberScrollState()
                LaunchedEffect(logs) { scrollState.animateScrollTo(scrollState.maxValue) }
                Text(text = logs, modifier = Modifier.padding(12.dp).verticalScroll(scrollState), color = Color(0xFF00FF00), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }
    }

    if (showPersonaSheet) {
        ModalBottomSheet(onDismissRequest = { showPersonaSheet = false }, sheetState = sheetState) {
            LazyColumn(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                item { Text("Select Architect Persona", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge) }
                item {
                    ListItem(
                        headlineContent = { Text("Standard Analyst (Default)") },
                        modifier = Modifier.clickable { selectedPersona = null; showPersonaSheet = false },
                        leadingContent = { Icon(Icons.Default.CheckCircle, null, tint = if (selectedPersona == null) MaterialTheme.colorScheme.primary else Color.Gray) }
                    )
                }
                items(personas) { persona ->
                    ListItem(
                        headlineContent = { Text(persona.name) },
                        supportingContent = { Text(persona.instructions, maxLines = 1) },
                        modifier = Modifier.clickable { selectedPersona = persona; showPersonaSheet = false },
                        leadingContent = { Icon(Icons.Default.AccountCircle, null, tint = if (selectedPersona?.name == persona.name) MaterialTheme.colorScheme.primary else Color.Gray) }
                    )
                }
            }
        }
    }
}