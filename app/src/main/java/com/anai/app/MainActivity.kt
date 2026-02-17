package com.anai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mock API Keys for rotation
        val keys = listOf("YOUR_KEY_1", "YOUR_KEY_2")

        setContent {
            MaterialTheme {
                Surface {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val mediaManager = remember { MediaManager(context) }
                    val geminiManager = remember { GeminiManager(context, keys, mediaManager) }

                    // Fixed: Calling the correct function name
                    DashboardScreen(geminiManager, mediaManager)
                }
            }
        }
    }
}