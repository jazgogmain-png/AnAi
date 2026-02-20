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
import com.anai.app.database.ArchitectDatabase
import com.anai.app.database.PlatformEntity
import kotlinx.coroutines.launch

// Navigation & UI Imports
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CenterAlignedTopAppBar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize Database & Managers
        val database = ArchitectDatabase.getDatabase(this)
        val dao = database.architectDao()
        val mediaManager = MediaManager(this)
        val geminiManager = GeminiManager(this, mediaManager, dao)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainAppScaffold(geminiManager, mediaManager, dao)
                }
            }
        }
    }
}

@Composable
fun MainAppScaffold(
    geminiManager: GeminiManager,
    mediaManager: MediaManager,
    dao: com.anai.app.database.ArchitectDao
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Observe Platforms from DB
    val dbPlatforms by dao.getAllPlatforms().collectAsState(initial = emptyList())

    // NAVIGATION STATE
    var currentScreen by remember { mutableStateOf("Architect") }
    var selectedPlatform by remember { mutableStateOf<PlatformEntity?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(12.dp))
                Text("ANAI ARCHITECT", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.headlineSmall)
                HorizontalDivider()

                Spacer(Modifier.height(8.dp))

                // 1. MASTER STUDIO (The Landing Page)
                NavigationDrawerItem(
                    label = { Text("Master Studio") },
                    selected = currentScreen == "Architect",
                    icon = { Icon(Icons.Default.Home, null) },
                    onClick = {
                        selectedPlatform = null
                        currentScreen = "Architect"
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // 2. DYNAMIC PLATFORMS (Manufactured in Blueprint Factory)
                if (dbPlatforms.isNotEmpty()) {
                    Text("DEDICATED STUDIOS", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp))
                }

                dbPlatforms.forEach { platform ->
                    NavigationDrawerItem(
                        label = { Text("${platform.name} Studio") },
                        selected = selectedPlatform?.id == platform.id && currentScreen == "Studio",
                        icon = { Icon(Icons.Default.PlayArrow, null) }, // Swapped from Movie to PlayArrow
                        onClick = {
                            selectedPlatform = platform
                            currentScreen = "Studio"
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                Spacer(Modifier.weight(1f)) // Push system items to bottom
                HorizontalDivider()

                // 3. SYSTEM TOOLS
                NavigationDrawerItem(
                    label = { Text("Blueprint Factory") },
                    selected = currentScreen == "Blueprints",
                    icon = { Icon(Icons.Default.Build, null) },
                    onClick = {
                        selectedPlatform = null
                        currentScreen = "Blueprints"
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Key Vault") },
                    selected = currentScreen == "Settings",
                    icon = { Icon(Icons.Default.Lock, null) },
                    onClick = {
                        selectedPlatform = null
                        currentScreen = "Settings"
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                AppTopBar(
                    title = when(currentScreen) {
                        "Architect" -> "Master Studio"
                        "Studio" -> "${selectedPlatform?.name} Studio"
                        "Blueprints" -> "Blueprint Factory"
                        "Settings" -> "Key Vault"
                        else -> "Video Architect"
                    },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (currentScreen) {
                    "Studio", "Architect" -> {
                        DashboardScreen(geminiManager, mediaManager, dao, selectedPlatform)
                    }
                    "Blueprints" -> BlueprintsScreen(dao)
                    "Settings" -> SettingsScreen(dao)
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