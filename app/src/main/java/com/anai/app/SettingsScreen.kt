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
import com.anai.app.database.KeyEntity
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(dao: ArchitectDao) {
    val scope = rememberCoroutineScope()
    var newKey by remember { mutableStateOf("") }
    val keys by dao.getAllKeys().collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Rotating Key Vault", style = MaterialTheme.typography.headlineSmall)
        Text("Add multiple keys to rotate and bypass quotas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = newKey,
            onValueChange = { newKey = it },
            label = { Text("Add Gemini API Key") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (newKey.isNotBlank()) {
                    scope.launch {
                        dao.insertKey(KeyEntity(key = newKey.trim()))
                        newKey = ""
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add to Rotation")
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(keys) { keyObj ->
                ListItem(
                    headlineContent = { Text("••••${keyObj.key.takeLast(4)}") },
                    trailingContent = {
                        IconButton(onClick = { scope.launch { dao.deleteKey(keyObj) } }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove")
                        }
                    }
                )
            }
        }
    }
}