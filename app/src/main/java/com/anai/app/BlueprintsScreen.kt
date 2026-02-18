package com.anai.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anai.app.database.ArchitectDao
import com.anai.app.database.PersonaEntity
import kotlinx.coroutines.launch

@Composable
fun BlueprintsScreen(dao: ArchitectDao) {
    val scope = rememberCoroutineScope()
    val personas by dao.getAllPersonas().collectAsState(initial = emptyList())

    var nameInput by remember { mutableStateOf("") }
    var promptInput by remember { mutableStateOf("") }

    // Using LazyColumn for the entire screen makes everything scrollable together
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Persona Factory", style = MaterialTheme.typography.headlineSmall)
            Text("Define Blueprints to automate Gemini's style.", style = MaterialTheme.typography.bodySmall)
        }

        // --- ADD NEW PERSONA SECTION ---
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Persona Name (e.g., TikTok Lola)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        label = { Text("Instructions") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 8 // Keeps the box from eating the whole screen
                    )
                    Button(
                        onClick = {
                            if (nameInput.isNotBlank() && promptInput.isNotBlank()) {
                                scope.launch {
                                    dao.savePersona(PersonaEntity(nameInput, promptInput))
                                    nameInput = ""
                                    promptInput = ""
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save Persona")
                    }
                }
            }
        }

        item {
            HorizontalDivider()
            Text("Stored Blueprints", style = MaterialTheme.typography.labelLarge)
        }

        // --- LIST OF PERSONAS ---
        items(personas) { persona ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(persona.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            persona.instructions,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3 // Prevents long prompts from making the list messy
                        )
                    }
                    IconButton(onClick = {
                        scope.launch { dao.deletePersona(persona.name) }
                    }) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}