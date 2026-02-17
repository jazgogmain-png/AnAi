package com.anai.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(keys: MutableList<String>) {
    var bulkKeysInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Gemini Key Vault",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Paste one or more keys (one per line).",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // --- BULK ADD SECTION ---
        OutlinedTextField(
            value = bulkKeysInput,
            onValueChange = { bulkKeysInput = it },
            label = { Text("Paste Keys Here (One Per Line)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5
        )

        Button(
            onClick = {
                if (bulkKeysInput.isNotBlank()) {
                    // Split by newline, trim whitespace, and filter out empty lines
                    val newKeys = bulkKeysInput.lines()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    keys.addAll(newKeys)
                    bulkKeysInput = ""
                }
            },
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add to Rotation")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- ACTIVE KEYS LIST ---
        Text(
            text = "Active Rotation (${keys.size} keys)",
            style = MaterialTheme.typography.titleMedium
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            itemsIndexed(keys) { index, key ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Key ${index + 1}: ${key.take(6)}...${key.takeLast(6)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        IconButton(onClick = { keys.removeAt(index) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}