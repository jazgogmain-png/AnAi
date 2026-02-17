package com.anai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anai.app.database.ArchitectDatabase // THE NEW IMPORT
import kotlinx.coroutines.launch

// --- MISSING NAVIGATION IMPORTS ---
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.rememberCoroutineScope

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Database once at App start
        val database = ArchitectDatabase.getDatabase(this)
        val dao = database.architectDao()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // Pass the DAO into the scaffold so managers can use it
                    MainAppScaffold(dao)
                }
            }
        }
    }
}

@Composable
fun MainAppScaffold(dao: com.anai.app.database.ArchitectDao) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    // --- APP STATE ---
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var currentScreen by remember { mutableStateOf("Architect") }

    // --- SHARED DATA ---
    // MutableStateList for real-time key rotation updates
    val keys = remember { mutableStateListOf<String>() }
    val mediaManager = remember { MediaManager(context) }

    // Updated GeminiManager will now take the DAO to save logs/analysis
    val geminiManager = remember { GeminiManager(context, keys, mediaManager, dao) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(12.dp))
                Text("AnAi Menu", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                HorizontalDivider()

                NavigationDrawerItem(
                    label = { Text("Architect") },
                    selected = currentScreen == "Architect",
                    icon = { Icon(Icons.Default.Build, contentDescription = null) },
                    onClick = { currentScreen = "Architect"; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Key Vault") },
                    selected = currentScreen == "Settings",
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    onClick = { currentScreen = "Settings"; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                AppTopBar(
                    title = if (currentScreen == "Architect") "AnAi Video Architect" else "Key Vault",
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (currentScreen) {
                    "Architect" -> DashboardScreen(geminiManager, mediaManager)
                    "Settings" -> SettingsScreen(keys)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: String, onMenuClick: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}