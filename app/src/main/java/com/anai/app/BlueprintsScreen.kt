package com.anai.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.anai.app.database.*
import kotlinx.coroutines.launch

@Composable
fun BlueprintsScreen(dao: ArchitectDao) {
    val scope = rememberCoroutineScope()

    // Platform Edit State
    var platId by remember { mutableIntStateOf(0) }
    var platName by remember { mutableStateOf("") }
    var hasDesc by remember { mutableStateOf(false) }
    var hasTags by remember { mutableStateOf(false) }

    // Persona Edit State
    var personaId by remember { mutableIntStateOf(0) }
    var personaName by remember { mutableStateOf("") }
    var personaInstructions by remember { mutableStateOf("") }

    // Engine Edit State
    var engineId by remember { mutableIntStateOf(0) }
    var engineName by remember { mutableStateOf("") }
    var engineInstructions by remember { mutableStateOf("") }
    var engineTemplate by remember { mutableStateOf("") }

    val platforms by dao.getAllPlatforms().collectAsState(initial = emptyList())
    val personas by dao.getAllPersonas().collectAsState(initial = emptyList())
    val engines by dao.getAllEngines().collectAsState(initial = emptyList())

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // --- SECTION: PLATFORMS ---
        item {
            Text(if (platId == 0) "Manufacture Platform" else "Edit Platform", color = Color.Magenta)
            OutlinedTextField(value = platName, onValueChange = { platName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = hasDesc, onCheckedChange = { hasDesc = it })
                Text("Desc", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(8.dp))
                Checkbox(checked = hasTags, onCheckedChange = { hasTags = it })
                Text("Tags", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                Button(onClick = {
                    scope.launch {
                        dao.insertPlatform(PlatformEntity(id = platId, name = platName, hasDescription = hasDesc, hasTags = hasTags))
                        platId = 0; platName = ""; hasDesc = false; hasTags = false
                    }
                }) { Text("Save") }
            }
        }
        items(platforms) { plat ->
            ListItem(
                headlineContent = { Text(plat.name) },
                modifier = Modifier.clickable { platId = plat.id; platName = plat.name; hasDesc = plat.hasDescription; hasTags = plat.hasTags },
                trailingContent = { IconButton(onClick = { scope.launch { dao.deletePlatform(plat) } }) { Icon(Icons.Default.Delete, null) } }
            )
        }

        item { HorizontalDivider(Modifier.padding(vertical = 16.dp)) }

        // --- SECTION: SOULS ---
        item {
            Text(if (personaId == 0) "Manufacture Soul" else "Edit Soul", color = Color.Cyan)
            OutlinedTextField(value = personaName, onValueChange = { personaName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = personaInstructions, onValueChange = { personaInstructions = it }, label = { Text("Instructions") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                scope.launch {
                    dao.insertPersona(PersonaEntity(id = personaId, name = personaName, instructions = personaInstructions))
                    personaId = 0; personaName = ""; personaInstructions = ""
                }
            }) { Text("Save Soul") }
            Spacer(Modifier.height(16.dp))
        }
        items(personas) { persona ->
            ListItem(
                headlineContent = { Text(persona.name) },
                modifier = Modifier.clickable { personaId = persona.id; personaName = persona.name; personaInstructions = persona.instructions },
                trailingContent = { IconButton(onClick = { scope.launch { dao.deletePersona(persona) } }) { Icon(Icons.Default.Delete, null) } }
            )
        }

        item { HorizontalDivider(Modifier.padding(vertical = 16.dp)) }

        // --- SECTION: ENGINES ---
        item {
            Text(if (engineId == 0) "Manufacture Script" else "Edit Script", color = Color.Yellow)
            OutlinedTextField(value = engineName, onValueChange = { engineName = it }, label = { Text("Engine Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = engineInstructions, onValueChange = { engineInstructions = it }, label = { Text("Instructions") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = engineTemplate, onValueChange = { engineTemplate = it }, label = { Text("Draft Template") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                scope.launch {
                    dao.insertEngine(EngineEntity(id = engineId, name = engineName, instructions = engineInstructions, draftTemplate = engineTemplate))
                    engineId = 0; engineName = ""; engineInstructions = ""; engineTemplate = ""
                }
            }) { Text("Save Engine") }
        }
        items(engines) { engine ->
            ListItem(
                headlineContent = { Text(engine.name) },
                modifier = Modifier.clickable { engineId = engine.id; engineName = engine.name; engineInstructions = engine.instructions; engineTemplate = engine.draftTemplate },
                trailingContent = { IconButton(onClick = { scope.launch { dao.deleteEngine(engine) } }) { Icon(Icons.Default.Delete, null) } }
            )
        }
    }
}