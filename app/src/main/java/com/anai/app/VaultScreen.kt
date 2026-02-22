package com.anai.app

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow // <-- FIXED: Added this import
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.anai.app.database.ArchitectDao
import com.anai.app.database.BlueprintEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(dao: ArchitectDao) {
    val scope = rememberCoroutineScope()
    val blueprints by dao.getAllHistory().collectAsState(initial = emptyList())
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Rename Dialog State
    var showRenameDialog by remember { mutableStateOf(false) }
    var blueprintToRename by remember { mutableStateOf<BlueprintEntity?>(null) }
    var newNameText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("SUCCESS VAULT", style = MaterialTheme.typography.headlineSmall, color = Color.Cyan)
        Text("Archived winning DNA and viral blueprints.", fontSize = 11.sp, color = Color.Gray)

        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(blueprints) { blueprint ->
                BlueprintCard(
                    blueprint = blueprint,
                    onDelete = { scope.launch { dao.deleteBlueprint(blueprint.id) } },
                    onStar = { scope.launch { dao.toggleStar(blueprint.id, !blueprint.isStarred) } },
                    onRename = {
                        blueprintToRename = blueprint
                        newNameText = blueprint.personaName
                        showRenameDialog = true
                    },
                    onCopy = {
                        val fullBio = "TITLE: ${blueprint.titleUsed}\nHOOK: ${blueprint.hookTimestamp}\nAURA: ${blueprint.auraProfile}\n\n${blueprint.fullDescription}"
                        clipboardManager.setText(AnnotatedString(fullBio))
                        Toast.makeText(context, "Blueprint Copied!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // --- THE RENAME DIALOG ---
    if (showRenameDialog && blueprintToRename != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Aura/Alias", fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = newNameText,
                    onValueChange = { newNameText = it },
                    label = { Text("Friendly Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        dao.updateBlueprintAlias(blueprintToRename!!.id, newNameText)
                        showRenameDialog = false
                        Toast.makeText(context, "Alias Updated!", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("SAVE", color = Color.Cyan) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("CANCEL") }
            }
        )
    }
}

@Composable
fun BlueprintCard(
    blueprint: BlueprintEntity,
    onDelete: () -> Unit,
    onStar: () -> Unit,
    onRename: () -> Unit,
    onCopy: () -> Unit
) {
    val dateStr = remember(blueprint.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        sdf.format(Date(blueprint.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, if (blueprint.isStarred) Color.Yellow.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // THUMBNAIL
                AsyncImage(
                    model = blueprint.thumbnailUri,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = blueprint.personaName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (blueprint.isStarred) Color.Yellow else Color.White
                        )
                        IconButton(onClick = onRename, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp), tint = Color.Cyan.copy(alpha = 0.6f))
                        }
                    }
                    Text(text = "${blueprint.platform} • $dateStr", fontSize = 10.sp, color = Color.Gray)
                }

                IconButton(onClick = onStar) {
                    Icon(
                        imageVector = if (blueprint.isStarred) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Star",
                        tint = if (blueprint.isStarred) Color.Yellow else Color.Gray
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // --- DATA PREVIEW ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column {
                    Text("📽️ CAPTION USED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text(blueprint.titleUsed, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column {
                    Text("✨ AURA PROFILE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Cyan)
                    Text(
                        text = blueprint.auraProfile,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // --- ACTION ROW ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp), tint = Color.Red.copy(alpha = 0.6f))
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onCopy,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy Full Bio", fontSize = 11.sp)
                }
            }
        }
    }
}