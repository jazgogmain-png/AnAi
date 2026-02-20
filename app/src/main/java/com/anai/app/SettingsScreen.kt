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

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Key Vault", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = newKey,
                onValueChange = { newKey = it },
                label = { Text("Add Gemini API Key") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                if (newKey.isNotBlank()) {
                    scope.launch {
                        dao.insertKey(KeyEntity(key = newKey.trim()))
                        newKey = ""
                    }
                }
            }) { Text("Vault Key") }
            Spacer(Modifier.height(16.dp))
        }

        items(keys) { keyObj ->
            ListItem(
                headlineContent = { Text("••••${keyObj.key.takeLast(4)}") },
                trailingContent = {
                    IconButton(onClick = { scope.launch { dao.deleteKey(keyObj) } }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    }
}