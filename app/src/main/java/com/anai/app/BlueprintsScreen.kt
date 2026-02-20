package com.anai.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anai.app.database.*
import kotlinx.coroutines.launch

@Composable
fun BlueprintsScreen(dao: ArchitectDao, geminiManager: GeminiManager) {
    val scope = rememberCoroutineScope()

    // Forge State
    var forgeInput by remember { mutableStateOf("") }
    var forgeResult by remember { mutableStateOf("") }
    val isProcessing by geminiManager.isProcessing.collectAsState()

    // Persona Edit State
    var personaId by remember { mutableIntStateOf(0) }
    var personaName by remember { mutableStateOf("") }
    var personaInstructions by remember { mutableStateOf("") }

    val personas by dao.getAllPersonas().collectAsState(initial = emptyList())

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        // --- 🧪 THE PERSONA FORGE (The Soul Lab) ---
        item {
            Text("PERSONA FORGE", style = MaterialTheme.typography.headlineSmall, color = Color.Yellow)
            Text("Describe a vibe, and let G3 build the soul DNA.", fontSize = 11.sp, color = Color.Gray)

            OutlinedTextField(
                value = forgeInput,
                onValueChange = { forgeInput = it },
                label = { Text("Describe the Soul") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                placeholder = { Text("e.g. A wholesome animal lover who uses emojis...") }
            )

            Button(
                onClick = {
                    scope.launch {
                        forgeResult = geminiManager.forgePersona(forgeInput)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = forgeInput.isNotBlank() && !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.Build, null)
                    Spacer(Modifier.width(8.dp))
                    Text("FORGE SOUL DNA")
                }
            }

            if (forgeResult.isNotBlank()) {
                Card(
                    modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("REFINED SOUL DNA:", style = MaterialTheme.typography.labelSmall, color = Color.Yellow)
                        Text(forgeResult, fontSize = 12.sp, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                personaInstructions = forgeResult
                                forgeResult = ""
                                forgeInput = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow, contentColor = Color.Black)
                        ) { Text("Apply to Blueprint") }
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        }

        // --- SECTION: MANUFACTURE SOULS ---
        item {
            Text(if (personaId == 0) "Manufacture Soul" else "Edit Soul", color = Color.Cyan)
            OutlinedTextField(
                value = personaName,
                onValueChange = { personaName = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = personaInstructions,
                onValueChange = { personaInstructions = it },
                label = { Text("Instructions") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Button(onClick = {
                    scope.launch {
                        dao.insertPersona(PersonaEntity(id = personaId, name = personaName, instructions = personaInstructions))
                        personaId = 0; personaName = ""; personaInstructions = ""
                    }
                }) { Text("Save Soul") }

                if (personaId != 0) {
                    TextButton(onClick = { personaId = 0; personaName = ""; personaInstructions = "" }) { Text("Cancel") }
                }
            }
        }

        items(personas) { persona ->
            ListItem(
                headlineContent = { Text(persona.name) },
                modifier = Modifier.clickable {
                    personaId = persona.id
                    personaName = persona.name
                    personaInstructions = persona.instructions
                },
                trailingContent = {
                    IconButton(onClick = { scope.launch { dao.deletePersona(persona) } }) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                    }
                }
            )
        }
    }
}