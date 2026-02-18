package com.anai.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(geminiManager: GeminiManager) {
    var chatInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val logs by geminiManager.uiLog.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Architect's Study",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        // --- CONVERSATION WINDOW ---
        Box(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                reverseLayout = false // Keeps the matrix feel scrolling down
            ) {
                item {
                    Text(
                        text = logs.substringBefore("###ARCHITECT_DRAFT###"), // Hides the technical draft block
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = Color(0xFF00FF00),
                        modifier = Modifier
                            .background(Color.Black, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }

        // --- MESSAGE INPUT ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = chatInput,
                onValueChange = { chatInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask Lola for a better hook...") },
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (chatInput.isNotBlank()) {
                        val msg = chatInput
                        chatInput = ""
                        scope.launch {
                            geminiManager.chat(msg)
                        }
                    }
                },
                colors = IconButtonDefaults.filledIconButtonColors()
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}