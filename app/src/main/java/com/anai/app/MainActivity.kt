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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
fun MainAppScaffold(geminiManager: GeminiManager, mediaManager: MediaManager, dao: com.anai.app.database.ArchitectDao) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val dbPlatforms by dao.getAllPlatforms().collectAsState(initial = emptyList())

    var currentScreen by remember { mutableStateOf("Architect") }
    var selectedPlatform by remember { mutableStateOf<PlatformEntity?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(12.dp))
                Text("ANAI ARCHITECT", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.headlineSmall)
                HorizontalDivider()

                NavigationDrawerItem(
                    label = { Text("Master Studio") },
                    selected = currentScreen == "Architect",
                    icon = { Icon(Icons.Default.Home, null) },
                    onClick = { selectedPlatform = null; currentScreen = "Architect"; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                dbPlatforms.forEach { platform ->
                    NavigationDrawerItem(
                        label = { Text("${platform.name} Studio") },
                        selected = selectedPlatform?.id == platform.id && currentScreen == "Studio",
                        icon = { Icon(Icons.Default.PlayArrow, null) },
                        onClick = { selectedPlatform = platform; currentScreen = "Studio"; scope.launch { drawerState.close() } },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // PROMPT LAB (VEO DNA EXTRACTION)
                NavigationDrawerItem(
                    label = { Text("Prompt Lab (VEO)") },
                    selected = currentScreen == "PromptLab",
                    icon = { Icon(Icons.Default.Build, null) },
                    onClick = { currentScreen = "PromptLab"; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // THE STUDY (BRAINSTORMING)
                NavigationDrawerItem(
                    label = { Text("The Study (Chat)") },
                    selected = currentScreen == "Chat",
                    icon = { Icon(Icons.Default.Send, null) },
                    onClick = { currentScreen = "Chat"; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(Modifier.weight(1f))

                // BLUEPRINT FACTORY (WHERE THE FORGE LIVES)
                NavigationDrawerItem(
                    label = { Text("Blueprint Factory") },
                    selected = currentScreen == "Blueprints",
                    icon = { Icon(Icons.Default.List, null) },
                    onClick = { currentScreen = "Blueprints"; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // KEY VAULT (ROTATION NODES)
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
                        "Studio" -> "${selectedPlatform?.name} Studio"
                        "PromptLab" -> "Prompt Lab"
                        "Chat" -> "The Study"
                        "Blueprints" -> "Blueprint Factory"
                        "Settings" -> "Key Vault"
                        else -> "Master Studio"
                    },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (currentScreen) {
                    "Studio", "Architect" -> DashboardScreen(geminiManager, mediaManager, dao, selectedPlatform)
                    "PromptLab" -> PromptLabScreen(geminiManager, mediaManager)
                    "Chat" -> ChatScreen(geminiManager)
                    // PASSING GEMINI MANAGER TO ENABLE THE FORGE
                    "Blueprints" -> BlueprintsScreen(dao, geminiManager)
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
        navigationIcon = { IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, null) } },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    )
}