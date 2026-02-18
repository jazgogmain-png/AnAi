package com.anai.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anai.app.database.ArchitectDao
import com.anai.app.database.EngineEntity
import com.anai.app.database.PersonaEntity
import kotlinx.coroutines.launch

@Composable
fun BlueprintsScreen(dao: ArchitectDao) {
    val scope = rememberCoroutineScope()

    // UI State for Tabs
    var activeTab by remember { mutableIntStateOf(0) } // 0: Persona, 1: Engine

    // Shared Editor State
    var selectedId by remember { mutableIntStateOf(0) }
    var nameInput by remember { mutableStateOf("") }
    var instrInput by remember { mutableStateOf("") }
    var templateInput by remember { mutableStateOf("") }

    // Database Flows
    val personas by dao.getAllPersonas().collectAsState(initial = emptyList())
    val engines by dao.getAllEngines().collectAsState(initial = emptyByList())

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Blueprint Factory", style = MaterialTheme.typography.headlineSmall)

            TabRow(selectedTabIndex = activeTab, modifier = Modifier.padding(vertical = 8.dp)) {
                Tab(selected = activeTab == 0, onClick = {
                    activeTab = 0
                    selectedId = 0; nameInput = ""; instrInput = ""; templateInput = ""
                }) {
                    Text("Soul (Personas)", modifier = Modifier.padding(8.dp))
                }
                Tab(selected = activeTab == 1, onClick = {
                    activeTab = 1
                    selectedId = 0; nameInput = ""; instrInput = ""; templateInput = ""
                }) {
                    Text("Script (Engines)", modifier = Modifier.padding(8.dp))
                }
            }
        }

        // --- DYNAMIC EDITOR SECTION ---
        item {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (selectedId == 0) "Create New" else "Editing: $nameInput",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Blueprint Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = instrInput,
                        onValueChange = { instrInput = it },
                        label = { Text("System Instructions") },
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )

                    if (activeTab == 1) {
                        OutlinedTextField(
                            value = templateInput,
                            onValueChange = { templateInput = it },
                            label = { Text("Draft Template (HT: MU: OV: etc.)") },
                            modifier = Modifier.fillMaxWidth().height(100.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (nameInput.isNotBlank()) {
                                scope.launch {
                                    if (activeTab == 0) {
                                        dao.savePersona(PersonaEntity(id = selectedId, name = nameInput, instructions = instrInput))
                                    } else {
                                        dao.saveEngine(EngineEntity(id = selectedId, name = nameInput, instructions = instrInput, draftTemplate = templateInput))
                                    }
                                    // Reset after save
                                    selectedId = 0; nameInput = ""; instrInput = ""; templateInput = ""
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(if (selectedId == 0) Icons.Default.Add else Icons.Default.Edit, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (selectedId == 0) "MANUFACTURE BLUEPRINT" else "UPDATE BLUEPRINT")
                    }

                    if (selectedId != 0) {
                        TextButton(onClick = { selectedId = 0; nameInput = ""; instrInput = ""; templateInput = "" }) {
                            Text("Cancel Editing", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        item { HorizontalDivider(); Text("Stored Blueprints", style = MaterialTheme.typography.labelLarge) }

        // --- LIST SECTION ---
        if (activeTab == 0) {
            items(personas) { persona ->
                BlueprintItem(
                    name = persona.name,
                    desc = persona.instructions,
                    onEdit = {
                        selectedId = persona.id
                        nameInput = persona.name
                        instrInput = persona.instructions
                    },
                    onDelete = { scope.launch { dao.deletePersona(persona) } }
                )
            }
        } else {
            items(engines) { engine ->
                BlueprintItem(
                    name = engine.name,
                    desc = engine.instructions,
                    onEdit = {
                        selectedId = engine.id
                        nameInput = engine.name
                        instrInput = engine.instructions
                        templateInput = engine.draftTemplate
                    },
                    onDelete = { scope.launch { dao.deleteEngine(engine) } }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
fun BlueprintItem(name: String, desc: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleSmall)
                Text(desc, style = MaterialTheme.typography.bodySmall, maxLines = 2, fontSize = 11.sp)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// Helper to handle empty state in collectAsState
private fun <T> emptyByList() = emptyList<T>()