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
import androidx.compose.runtime.rememberCoroutineScope

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = ArchitectDatabase.getDatabase(this)
        val dao = database.architectDao()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // NAVIGATION STATE
    var currentScreen by remember { mutableStateOf("Architect") }

    // SHARED APP STATE (Now managing persistence through DAO)
    val mediaManager = remember { MediaManager(context) }
    val geminiManager = remember { GeminiManager(context, mediaManager, dao) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(12.dp))
                Text("ANAI ARCHITECT", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.headlineSmall)
                HorizontalDivider()

                // NAVIGATION ITEMS
                NavigationDrawerItem(
                    label = { Text("Video Architect") },
                    selected = currentScreen == "Architect",
                    icon = { Icon(Icons.Default.Build, null) },
                    onClick = { currentScreen = "Architect"; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Blueprints (Personas)") },
                    selected = currentScreen == "Blueprints",
                    icon = { Icon(Icons.Default.List, null) },
                    onClick = { currentScreen = "Blueprints"; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("The Study (Chat)") },
                    selected = currentScreen == "Chat",
                    icon = { Icon(Icons.Default.Send, null) },
                    onClick = { currentScreen = "Chat"; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Key Vault") },
                    selected = currentScreen == "Settings",
                    icon = { Icon(Icons.Default.Lock, null) },
                    onClick = { currentScreen = "Settings"; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                AppTopBar(
                    title = when(currentScreen) {
                        "Architect" -> "Video Architect"
                        "Blueprints" -> "Blueprint Factory"
                        "Chat" -> "The Study"
                        else -> "Key Vault"
                    },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (currentScreen) {
                    "Architect" -> DashboardScreen(geminiManager, mediaManager, dao)
                    "Blueprints" -> BlueprintsScreen(dao)
                    "Chat" -> ChatScreen(geminiManager)
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