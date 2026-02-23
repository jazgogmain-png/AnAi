package com.anai.app

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.anai.app.database.BlueprintEntity
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

    var videoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPersona by remember { mutableStateOf<PersonaEntity?>(null) }
    var selectedEngine by remember { mutableStateOf<EngineEntity?>(null) }
    var activeCaptionIndex by rememberSaveable { mutableIntStateOf(0) }

    val isTikTok = selectedEngine?.name?.contains("TikTok", ignoreCase = true) ?: false
    val isYouTube = selectedEngine?.name?.contains("YouTube", ignoreCase = true) ?: (!isTikTok && selectedEngine != null)

    // Collect Terminal Logs from GeminiManager
    val isProcessing by geminiManager.isProcessing.collectAsState()
    val statusLogs by geminiManager.statusLogs.collectAsState() // Ensure this exists in GeminiManager

    LaunchedEffect(selectedPlatform, engines, personas) {
        if (selectedPlatform != null) {
            selectedEngine = engines.find { it.name.contains(selectedPlatform.name, ignoreCase = true) }
                ?: engines.firstOrNull()
            if (selectedPersona == null) selectedPersona = personas.firstOrNull()
        }
    }

    val captionOptions by geminiManager.captionOptions.collectAsState()
    val seoTags by geminiManager.seoTags.collectAsState()
    val descPart by geminiManager.descPart.collectAsState()
    val hashtagPart by geminiManager.hashtagPart.collectAsState()
    val hookPart by geminiManager.hookPart.collectAsState()
    val auraPart by geminiManager.auraPart.collectAsState()

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) {}
            videoUriString = it.toString()
            mediaManager.loadVideo(it)
            geminiManager.clearLog()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { ArchitectHeader(geminiManager, mediaManager, videoUriString) }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = if (selectedEngine != null) "${selectedEngine?.name} Studio" else "Master Studio", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                    Row {
                        if (videoUriString == null) {
                            IconButton(onClick = { videoPicker.launch(arrayOf("video/*")) }) {
                                Icon(Icons.Default.Add, null, tint = Color.Cyan)
                            }
                        }
                        IconButton(onClick = { videoUriString = null; geminiManager.clearLog() }) {
                            Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Refresh", modifier = Modifier.size(20.dp))
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
                            FilterChip(
                                selected = selectedEngine?.id == engine.id,
                                onClick = { selectedEngine = engine },
                                label = { Text(engine.name, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium).padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isTikTok) "TikTok Verbal" else "YouTube SEO Block", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.weight(1f))
                        listOf("V1", "V2", "V3").forEachIndexed { index, label ->
                            InputChip(
                                selected = activeCaptionIndex == index,
                                onClick = { activeCaptionIndex = index },
                                label = { Text(label, fontSize = 9.sp) },
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    val currentCap = if (captionOptions.size > activeCaptionIndex) captionOptions[activeCaptionIndex] else ""
                    val formattedHashtags = hashtagPart.split(",").filter { it.isNotBlank() }.joinToString(" ") {
                        val trimmed = it.trim()
                        if (trimmed.startsWith("#")) trimmed else "#$trimmed"
                    }

                    val fullVerbalBlock = when {
                        isTikTok -> if (formattedHashtags.isNotBlank()) "$currentCap\n\n$formattedHashtags" else currentCap
                        isYouTube -> buildString {
                            append(currentCap)
                            append("\n\n")
                            append(descPart)
                            if (formattedHashtags.isNotBlank()) append("\n\n$formattedHashtags")
                        }
                        else -> currentCap
                    }

                    OutlinedTextField(
                        value = fullVerbalBlock, onValueChange = {}, readOnly = true,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp).pointerInput(Unit) {
                            detectTapGestures(onDoubleTap = {
                                if (fullVerbalBlock.isNotBlank()) {
                                    clipboardManager.setText(AnnotatedString(fullVerbalBlock))
                                    Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                                }
                            })
                        },
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item {
                AnimatedVisibility(visible = auraPart.isNotBlank() || hookPart.isNotBlank()) {
                    Column(modifier = Modifier.fillMaxWidth().border(1.dp, Color.Cyan.copy(alpha = 0.5f), MaterialTheme.shapes.medium).padding(12.dp)) {
                        Text("Visual Strategy", style = MaterialTheme.typography.labelLarge, color = Color.Cyan)
                        if (hookPart.isNotBlank()) {
                            Text("🎯 HOOK: $hookPart", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        }
                        if (auraPart.isNotBlank()) {
                            Text("✨ AURA: $auraPart", fontSize = 12.sp, color = Color.LightGray, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }

            item {
                AnimatedVisibility(visible = descPart.isNotBlank() || seoTags.isNotBlank()) {
                    if (isTikTok) {
                        val productionNotes = buildString {
                            if (descPart.isNotBlank()) append(descPart)
                            if (descPart.isNotBlank() && seoTags.isNotBlank()) append("\n\n")
                            if (seoTags.isNotBlank()) append(seoTags)
                        }
                        Column(modifier = Modifier.fillMaxWidth().border(1.dp, Color.Magenta.copy(alpha = 0.5f), MaterialTheme.shapes.medium).padding(12.dp)) {
                            Text("Production Notes", style = MaterialTheme.typography.labelLarge, color = Color.Magenta)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = productionNotes, onValueChange = {}, readOnly = true,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                                textStyle = MaterialTheme.typography.bodySmall,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent)
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth().border(1.dp, Color.Yellow.copy(alpha = 0.5f), MaterialTheme.shapes.medium).padding(12.dp)) {
                            Text("Individual SEO Tags", style = MaterialTheme.typography.labelLarge, color = Color.Yellow)
                            Spacer(Modifier.height(8.dp))
                            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                seoTags.split(",").filter { it.isNotBlank() }.forEach { tag ->
                                    val cleanTag = tag.trim()
                                    SuggestionChip(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(cleanTag))
                                            Toast.makeText(context, "Copied Tag!", Toast.LENGTH_SHORT).show()
                                        },
                                        label = { Text(cleanTag, fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                geminiManager.analyzeVideo("", selectedEngine?.name ?: "Master", selectedPersona?.name, selectedPersona?.instructions, videoUriString, selectedEngine?.instructions, selectedEngine?.draftTemplate)
                            }
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        enabled = selectedEngine != null && selectedPersona != null && videoUriString != null && !isProcessing
                    ) {
                        Text("EXECUTE SCAN", fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = {
                            scope.launch {
                                val currentCap = if (captionOptions.size > activeCaptionIndex) captionOptions[activeCaptionIndex] else ""
                                val thumbPath = videoUriString?.let { uriStr ->
                                    mediaManager.snatchThumbnail(Uri.parse(uriStr))
                                }

                                val blueprint = BlueprintEntity(
                                    videoUri = videoUriString ?: "",
                                    thumbnailUri = thumbPath,
                                    personaName = selectedPersona?.name ?: "Unknown",
                                    platform = selectedEngine?.name ?: "Master",
                                    titleUsed = currentCap,
                                    hookTimestamp = hookPart,
                                    auraProfile = auraPart,
                                    fullDescription = descPart,
                                    entryType = "SCAN" // 🚀 Tagging as SCAN
                                )
                                dao.insertBlueprint(blueprint)
                                Toast.makeText(context, "Blueprint Vaulted!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.height(56.dp).width(56.dp).border(1.dp, Color.Yellow.copy(alpha = 0.5f), MaterialTheme.shapes.medium),
                        enabled = (descPart.isNotBlank() || hookPart.isNotBlank()) && !isProcessing
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Vault", tint = Color.Yellow)
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }

        // 🦾 THE MATRIX OVERLAY (KITT + Terminal)
        if (isProcessing) {
            MatrixTerminalOverlay(statusLogs)
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

@Composable
fun MatrixTerminalOverlay(logs: List<String>) {
    val scrollState = rememberLazyListState()

    // KITT Scanner Animation
    val infiniteTransition = rememberInfiniteTransition(label = "KITT")
    val scanOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "KITT_SCAN"
    )

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) scrollState.animateScrollToItem(logs.size - 1)
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)).clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.85f).fillMaxHeight(0.6f)
                .border(2.dp, Color.Green.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .background(Color.Black, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            // KITT Scanner Header
            Box(
                modifier = Modifier.fillMaxWidth().height(4.dp).background(Color.DarkGray.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(0.3f).fillMaxHeight()
                        .align(Alignment.Center)
                        .offset(x = (scanOffset * 100).dp)
                        .background(Brush.horizontalGradient(listOf(Color.Transparent, Color.Red, Color.Transparent)))
                )
            }

            Text(
                "ANAI_ARCHITECT_LOG",
                color = Color.Green,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            )

            // Live Terminal Feed
            LazyColumn(state = scrollState, modifier = Modifier.weight(1f)) {
                items(logs) { log ->
                    Text(
                        text = "> $log",
                        color = Color.Green.copy(alpha = 0.8f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                color = Color.Green,
                trackColor = Color.Green.copy(alpha = 0.1f)
            )
        }
    }
}