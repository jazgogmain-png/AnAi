package com.anai.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

    // Engine Edit State
    var engineId by remember { mutableIntStateOf(0) }
    var engineName by remember { mutableStateOf("") }
    var engineInstructions by remember { mutableStateOf("") }
    var engineTemplate by remember { mutableStateOf("") }

    val personas by dao.getAllPersonas().collectAsState(initial = emptyList())
    val engines by dao.getAllEngines().collectAsState(initial = emptyList())

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        // --- 🧪 THE PERSONA FORGE ---
        item {
            Text("PERSONA FORGE", style = MaterialTheme.typography.headlineSmall, color = Color.Yellow)
            OutlinedTextField(
                value = forgeInput,
                onValueChange = { forgeInput = it },
                label = { Text("Describe the Soul") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
            Button(
                onClick = { scope.launch { forgeResult = geminiManager.forgePersona(forgeInput) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = forgeInput.isNotBlank() && !isProcessing
            ) {
                if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                else Text("FORGE SOUL DNA")
            }
            if (forgeResult.isNotBlank()) {
                Card(modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.DarkGray)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(forgeResult, fontSize = 12.sp, color = Color.White)
                        Button(onClick = { personaInstructions = forgeResult; forgeResult = "" }) { Text("Apply to Soul") }
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        }

        // --- 🧬 MANUFACTURE SOULS (PERSONAS) ---
        item {
            Text("MANUFACTURE SOUL", color = Color.Cyan, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = personaName, onValueChange = { personaName = it }, label = { Text("Soul Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = personaInstructions, onValueChange = { personaInstructions = it }, label = { Text("Instructions") }, modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp))
            Button(onClick = {
                scope.launch {
                    dao.insertPersona(PersonaEntity(id = personaId, name = personaName, instructions = personaInstructions))
                    personaId = 0; personaName = ""; personaInstructions = ""
                }
            }, modifier = Modifier.padding(top = 8.dp)) { Text("Save Soul") }
        }
        items(personas) { persona ->
            ListItem(
                headlineContent = { Text(persona.name) },
                modifier = Modifier.clickable { personaId = persona.id; personaName = persona.name; personaInstructions = persona.instructions },
                trailingContent = { IconButton(onClick = { scope.launch { dao.deletePersona(persona) } }) { Icon(Icons.Default.Delete, null) } }
            )
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp)) }

        // --- ⚙️ MANUFACTURE SCRIPTS (ENGINES) ---
        item {
            Text("MANUFACTURE ENGINE", color = Color.Green, style = MaterialTheme.typography.titleMedium)
            Text("This is the structural mold for the platform.", fontSize = 11.sp, color = Color.Gray)
            OutlinedTextField(value = engineName, onValueChange = { engineName = it }, label = { Text("Engine Name (e.g. YouTube Viral)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = engineInstructions, onValueChange = { engineInstructions = it }, label = { Text("Logic/Instructions") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = engineTemplate, onValueChange = { engineTemplate = it }, label = { Text("Output Template") }, modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Button(onClick = {
                    scope.launch {
                        dao.insertEngine(EngineEntity(id = engineId, name = engineName, instructions = engineInstructions, draftTemplate = engineTemplate))
                        engineId = 0; engineName = ""; engineInstructions = ""; engineTemplate = ""
                    }
                }) { Text(if (engineId == 0) "Save Engine" else "Update Engine") }
                if (engineId != 0) TextButton(onClick = { engineId = 0; engineName = ""; engineInstructions = ""; engineTemplate = "" }) { Text("Cancel") }
            }
        }
        items(engines) { engine ->
            ListItem(
                headlineContent = { Text(engine.name) },
                modifier = Modifier.clickable {
                    engineId = engine.id
                    engineName = engine.name
                    engineInstructions = engine.instructions
                    engineTemplate = engine.draftTemplate
                },
                trailingContent = { IconButton(onClick = { scope.launch { dao.deleteEngine(engine) } }) { Icon(Icons.Default.Delete, null) } }
            )
        }

        item { Spacer(Modifier.height(100.dp)) } // Leave room to scroll past the bottom
    }
}