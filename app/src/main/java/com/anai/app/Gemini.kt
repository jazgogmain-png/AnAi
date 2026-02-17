package com.anai.app

import android.content.Context
import android.net.Uri
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class GeminiManager(
    private val context: Context,
    private val apiKeys: List<String>,
    private val mediaManager: MediaManager
) {
    private var currentKeyIndex = 0
    private val _uiLog = MutableStateFlow("[SYSTEM]: Rotation Manager Active.")
    val uiLog = _uiLog.asStateFlow()

    private fun getNextModel(): GenerativeModel {
        val key = apiKeys[currentKeyIndex]
        currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size
        return GenerativeModel(
            modelName = "gemini-3-flash-preview",
            apiKey = key,
            requestOptions = RequestOptions(apiVersion = "v1beta")
        )
    }

    suspend fun analyzeVideo(contextInfo: String, platform: String, videoUriString: String?) {
        if (videoUriString == null) return
        val videoUri = Uri.parse(videoUriString)

        updateLog("Starting Squeezer...")
        val squeezedFile = mediaManager.squeezeVideoForAi(videoUri) { updateLog(it) }

        // Grab the bytes safely (The Lola Method)
        val videoBytes = try {
            if (squeezedFile != null && squeezedFile.exists()) {
                squeezedFile.readBytes()
            } else {
                context.contentResolver.openInputStream(videoUri)?.use { it.readBytes() }
            }
        } catch (e: Exception) {
            updateLog("BLOB_ERR: ${e.message}")
            null
        }

        if (videoBytes == null) {
            updateLog("ERROR: Video pixels are empty.")
            return
        }

        var success = false
        var attempt = 0

        while (attempt < apiKeys.size && !success) {
            val model = getNextModel()
            try {
                updateLog("Key ${currentKeyIndex + 1} Scanning Pixels...")

                val response = withContext(Dispatchers.IO) {
                    model.generateContent(
                        content {
                            // This is the core we need
                            blob("video/mp4", videoBytes)
                            text("Analyze for $platform. User says: $contextInfo. Task: Apply Lola viral formula.")
                        }
                    )
                }

                if (response.text != null) {
                    updateLog("SUCCESS: G3 analysis ready!")
                    success = true
                }
            } catch (e: Exception) {
                attempt++
                updateLog("ERR: ${e.message}. Rotating keys...")
                delay(2000)
            }
        }
    }

    private fun updateLog(msg: String) {
        _uiLog.value += "\n[LOG]: $msg"
    }
}