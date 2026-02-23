package com.anai.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anai.app.database.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(dao: ArchitectDao) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    var newKey by remember { mutableStateOf("") }
    val keys by dao.getAllKeys().collectAsState(initial = emptyList())

    // Lifeboat State
    var importText by remember { mutableStateOf("") }

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

        // --- LIFEBOAT SECTION ---
        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(16.dp))
        Text("SYSTEM LIFEBOAT", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFFD700)) // Gold
        Text("Export or Restore Souls, Engines, Keys, and Vault.", style = MaterialTheme.typography.bodySmall)

        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            // EXPORT BUTTON
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val backupMap = mapOf(
                                "souls" to dao.getAllPersonasDirect(),
                                "engines" to dao.getAllEnginesDirect(),
                                "keys" to dao.getAllKeysDirect(),
                                "vault" to dao.getAllHistoryDirect()
                            )
                            val json = Gson().toJson(backupMap)
                            val clip = ClipData.newPlainText("AnAi_Backup", json)
                            clipboardManager.setPrimaryClip(clip)
                            Toast.makeText(context, "Lifeboat Manifest Copied to Clipboard!", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Export Failed!", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Share, null)
                Spacer(Modifier.width(8.dp))
                Text("Export")
            }

            Spacer(Modifier.width(8.dp))

            // RESTORE BUTTON
            Button(
                onClick = {
                    if (importText.isNotBlank()) {
                        scope.launch {
                            try {
                                val type = object : TypeToken<Map<String, List<Any>>>() {}.type
                                val data: Map<String, List<Any>> = Gson().fromJson(importText, type)

                                // 1. Restore API Keys
                                data["keys"]?.let { list ->
                                    val json = Gson().toJson(list)
                                    val restored: List<KeyEntity> = Gson().fromJson(json, object : TypeToken<List<KeyEntity>>() {}.type)
                                    restored.forEach { dao.insertKey(it) }
                                }

                                // 2. Restore Souls
                                data["souls"]?.let { list ->
                                    val json = Gson().toJson(list)
                                    val restored: List<PersonaEntity> = Gson().fromJson(json, object : TypeToken<List<PersonaEntity>>() {}.type)
                                    restored.forEach { dao.insertPersona(it) }
                                }

                                // 3. Restore Engines
                                data["engines"]?.let { list ->
                                    val json = Gson().toJson(list)
                                    val restored: List<EngineEntity> = Gson().fromJson(json, object : TypeToken<List<EngineEntity>>() {}.type)
                                    restored.forEach { dao.insertEngine(it) }
                                }

                                // 4. Restore Vault
                                data["vault"]?.let { list ->
                                    val json = Gson().toJson(list)
                                    val restored: List<BlueprintEntity> = Gson().fromJson(json, object : TypeToken<List<BlueprintEntity>>() {}.type)
                                    restored.forEach { dao.insertBlueprint(it) }
                                }

                                Toast.makeText(context, "System Fully Restored!", Toast.LENGTH_LONG).show()
                                importText = ""
                            } catch (e: Exception) {
                                Toast.makeText(context, "Restore Failure: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Filled.Undo, contentDescription = "Restore")
                Spacer(Modifier.width(8.dp))
                Text("Restore")
            }
        }

        OutlinedTextField(
            value = importText,
            onValueChange = { importText = it },
            label = { Text("Paste Lifeboat JSON here to Restore") },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall
        )

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
