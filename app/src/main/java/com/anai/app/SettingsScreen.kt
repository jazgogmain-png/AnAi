package com.anai.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anai.app.database.ArchitectDao
import com.anai.app.database.KeyEntity
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun SettingsScreen(dao: ArchitectDao) {
    var bulkKeysInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val savedKeys by dao.getAllKeys().collectAsState(initial = emptyList())
    var showNuclearDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Key Vault", style = MaterialTheme.typography.headlineSmall)
        Text("Your keys are now saved to the local database.", style = MaterialTheme.typography.bodySmall)

        // --- BULK ADD ---
        OutlinedTextField(
            value = bulkKeysInput,
            onValueChange = { bulkKeysInput = it },
            label = { Text("Paste Keys (One Per Line)") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        )

        Button(
            onClick = {
                val keys = bulkKeysInput.lines().filter { it.isNotBlank() }
                scope.launch {
                    keys.forEach {
                        // FIXED: Matches the (ID, Key) structure
                        dao.saveKey(KeyEntity(key = it.trim()))
                    }
                    bulkKeysInput = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("ADD KEYS TO VAULT")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // --- LIST OF KEYS ---
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(savedKeys) { keyObj ->
                ListItem(
                    headlineContent = { Text(keyObj.key.take(15) + "...") },
                    trailingContent = {
                        IconButton(onClick = {
                            scope.launch {
                                // FIXED: Passes the whole Entity
                                dao.deleteKey(keyObj)
                            }
                        }) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            }
        }

        // --- NUCLEAR RESET ---
        Button(
            onClick = { showNuclearDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.Delete, null)
            Spacer(Modifier.width(8.dp))
            Text("Clear History (Nuclear)")
        }

        if (showNuclearDialog) {
            NuclearResetDialog(
                onConfirm = { scope.launch { dao.nukeHistory() }; showNuclearDialog = false },
                onDismiss = { showNuclearDialog = false }
            )
        }
    }
}

@Composable
fun NuclearResetDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val safetyCode = remember { Random.nextInt(1000, 9999).toString() }
    var inputCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Nuclear Wipe") },
        text = {
            Column {
                Text("This deletes history only. Enter code: $safetyCode")
                OutlinedTextField(
                    value = inputCode,
                    onValueChange = { inputCode = it },
                    label = { Text("Safety Code") },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                enabled = inputCode == safetyCode,
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("WIPE") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}