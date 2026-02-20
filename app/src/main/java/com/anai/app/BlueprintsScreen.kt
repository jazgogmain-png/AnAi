package com.anai.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anai.app.database.ArchitectDao
import com.anai.app.database.EngineEntity
import com.anai.app.database.PersonaEntity
import com.anai.app.database.PlatformEntity
import kotlinx.coroutines.launch

@Composable
fun BlueprintsScreen(dao: ArchitectDao) {
    val scope = rememberCoroutineScope()

    // States for Persona
    var personaName by remember { mutableStateOf("") }
    var personaInstructions by remember { mutableStateOf("") }

    // States for Engine
    var engineName by remember { mutableStateOf("") }
    var engineInstructions by remember { mutableStateOf("") }
    var engineTemplate by remember { mutableStateOf("") }

    // States for Platform
    var platformName by remember { mutableStateOf("") }
    var hasDescription by remember { mutableStateOf(false) }
    var hasTags by remember { mutableStateOf(false) }

    val personas by dao.getAllPersonas().collectAsState(initial = emptyList())
    val engines by dao.getAllEngines().collectAsState(initial = emptyList())
    val platforms by dao.getAllPlatforms().collectAsState(initial = emptyList())

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // --- SECTION: PLATFORMS ---
        item {
            Text("Manufacture Platform", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(value = platformName, onValueChange = { platformName = it }, label = { Text("Platform Name (YouTube, TikTok...)") }, modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row { Checkbox(checked = hasDescription, onCheckedChange = { hasDescription = it }); Text("Has Description") }
                Row { Checkbox(checked = hasTags, onCheckedChange = { hasTags = it }); Text("Has Tags") }
            }
            Button(onClick = {
                scope.launch {
                    dao.insertPlatform(PlatformEntity(name = platformName, hasDescription = hasDescription, hasTags = hasTags))
                    platformName = ""
                }
            }) { Text("Save Platform") }
            Spacer(Modifier.height(16.dp))
        }

        items(platforms) { platform ->
            ListItem(
                headlineContent = { Text(platform.name) },
                supportingContent = { Text("Desc: ${platform.hasDescription} | Tags: ${platform.hasTags}") },
                trailingContent = {
                    IconButton(onClick = { scope.launch { dao.deletePlatform(platform) } }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        }

        item { HorizontalDivider(Modifier.padding(vertical = 16.dp)) }

        // --- SECTION: PERSONAS ---
        item {
            Text("Manufacture Soul (Persona)", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(value = personaName, onValueChange = { personaName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = personaInstructions, onValueChange = { personaInstructions = it }, label = { Text("Instructions") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                scope.launch {
                    dao.insertPersona(PersonaEntity(name = personaName, instructions = personaInstructions))
                    personaName = ""
                    personaInstructions = ""
                }
            }) { Text("Save Soul") }
            Spacer(Modifier.height(16.dp))
        }

        items(personas) { persona ->
            ListItem(
                headlineContent = { Text(persona.name) },
                trailingContent = {
                    IconButton(onClick = { scope.launch { dao.deletePersona(persona) } }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        }

        item { HorizontalDivider(Modifier.padding(vertical = 16.dp)) }

        // --- SECTION: ENGINES ---
        item {
            Text("Manufacture Script (Engine)", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(value = engineName, onValueChange = { engineName = it }, label = { Text("Engine Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = engineInstructions, onValueChange = { engineInstructions = it }, label = { Text("Instructions") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = engineTemplate, onValueChange = { engineTemplate = it }, label = { Text("Draft Template") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                scope.launch {
                    dao.insertEngine(EngineEntity(name = engineName, instructions = engineInstructions, draftTemplate = engineTemplate))
                    engineName = ""
                    engineInstructions = ""
                    engineTemplate = ""
                }
            }) { Text("Save Engine") }
            Spacer(Modifier.height(16.dp))
        }

        items(engines) { engine ->
            ListItem(
                headlineContent = { Text(engine.name) },
                trailingContent = {
                    IconButton(onClick = { scope.launch { dao.deleteEngine(engine) } }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    }
}