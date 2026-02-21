package com.anai.app

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
    val history by dao.getAllHistory().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "SUCCESS VAULT",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Cyan
        )
        Text(
            text = "Archived viral blueprints and visual auras.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        Spacer(Modifier.height(16.dp))

        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Vault Empty. Star a scan to begin.", color = Color.DarkGray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(history) { blueprint ->
                    VaultCard(
                        blueprint = blueprint,
                        onDelete = { scope.launch { dao.deleteBlueprint(blueprint.id) } },
                        onToggleStar = { scope.launch { dao.toggleStar(blueprint.id, !blueprint.isStarred) } },
                        onCopy = {
                            clipboard.setText(AnnotatedString(blueprint.fullDescription))
                            Toast.makeText(context, "Description Copied!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun VaultCard(
    blueprint: BlueprintEntity,
    onDelete: () -> Unit,
    onToggleStar: () -> Unit,
    onCopy: () -> Unit
) {
    val date = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(blueprint.timestamp))

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        border = if (blueprint.isStarred) BorderStroke(1.dp, Color.Yellow) else BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // --- HEADER ROW (Persona & Date) ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (blueprint.thumbnailUri != null) {
                    AsyncImage(
                        model = blueprint.thumbnailUri,
                        contentDescription = null,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = blueprint.personaName, fontWeight = FontWeight.Bold, color = Color.Cyan)
                    Text(text = "$date • ${blueprint.platform}", fontSize = 10.sp, color = Color.Gray)
                }

                IconButton(onClick = onToggleStar) {
                    Icon(
                        imageVector = if (blueprint.isStarred) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (blueprint.isStarred) Color.Yellow else Color.Gray
                    )
                }
            }

            // --- TITLE / CAPTION ---
            if (blueprint.titleUsed.isNotBlank()) {
                Text(
                    text = blueprint.titleUsed,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    fontWeight = FontWeight.Medium
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // --- REFINED AUDIT SECTION (Full Width, No Collapse) ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column {
                    Text("🎯 GOLDEN HOOK", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Yellow)
                    Text(
                        text = blueprint.hookTimestamp,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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