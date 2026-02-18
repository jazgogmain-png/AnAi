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

        // --- INPUT SECTION ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Persona Name (e.g. Lola)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    label = { Text("Instructions (How should it act?)") },
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
                Button(
                    onClick = {
                        if (nameInput.isNotBlank() && promptInput.isNotBlank()) {
                            scope.launch {
                                // FIXED: Matches the new (ID, Name, Instructions) structure
                                dao.savePersona(PersonaEntity(name = nameInput, instructions = promptInput))
                                nameInput = ""
                                promptInput = ""
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("SAVE BLUEPRINT")
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
                            maxLines = 3
                        )
                    }
                    IconButton(onClick = {
                        scope.launch {
                            // FIXED: Now passes the whole Entity to the delete function
                            dao.deletePersona(persona)
                        }
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