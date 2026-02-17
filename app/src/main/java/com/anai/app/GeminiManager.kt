package com.anai.app

import android.content.Context
import android.net.Uri
import com.anai.app.database.ArchitectDao
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class GeminiManager(
    private val context: Context,
    private val apiKeys: List<String>,
    private val mediaManager: MediaManager,
    private val dao: ArchitectDao
) {
    private var currentKeyIndex = 0
    private val _uiLog = MutableStateFlow("[SYSTEM]: Architect Manager Active.")
    val uiLog = _uiLog.asStateFlow()

    private fun getNextModel(): GenerativeModel {
        if (apiKeys.isEmpty()) throw IllegalStateException("No API Keys in Vault!")
        val key = apiKeys[currentKeyIndex]
        currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size
        return GenerativeModel(
            modelName = "gemini-3-flash-preview",
            apiKey = key,
            requestOptions = RequestOptions(apiVersion = "v1beta")
        )
    }

    // MODE 1: Video Analysis (Pixel Scan)
    suspend fun analyzeVideo(contextInfo: String, platform: String, videoUriString: String?) {
        if (videoUriString == null) return
        val videoUri = Uri.parse(videoUriString)

        updateLog("Scanning Video Pixels for $platform...")
        val videoBytes = context.contentResolver.openInputStream(videoUri)?.use { it.readBytes() }

        runWithRotation { model ->
            model.generateContent(content {
                videoBytes?.let { blob("video/mp4", it) }
                text("Platform: $platform. Context: $contextInfo. Task: Analyze visual hooks and provide 3 viral caption options.")
            })
        }
    }

    // MODE 2: Stats Analysis (Screenshot Scan)
    suspend fun analyzeStats(screenshotUri: Uri, userNotes: String) {
        updateLog("Analyzing Performance Screenshot...")
        val imageBytes = context.contentResolver.openInputStream(screenshotUri)?.use { it.readBytes() }

        runWithRotation { model ->
            model.generateContent(content {
                imageBytes?.let { blob("image/png", it) }
                text("Analyze these analytics stats. User Notes: $userNotes. Goal: Provide data-driven advice to improve the next video.")
            })
        }
    }

    // MODE 3: The Study (Direct Chat)
    suspend fun chat(message: String) {
        updateLog("Consulting Architect: $message")
        runWithRotation { model ->
            model.generateContent(message)
        }
    }

    private suspend fun runWithRotation(block: suspend (GenerativeModel) -> com.google.ai.client.generativeai.type.GenerateContentResponse) {
        var success = false
        var attempt = 0
        while (attempt < apiKeys.size && !success) {
            try {
                val model = getNextModel()
                val response = withContext(Dispatchers.IO) { block(model) }
                response.text?.let {
                    updateLog("G3 ARCHITECT: $it")
                    success = true
                }
            } catch (e: Exception) {
                attempt++
                val errorMsg = e.message ?: "Unknown Error"
                updateLog("Rotation: Key $currentKeyIndex failed ($errorMsg). Retrying with next key...")
                delay(1000)
            }
        }
        if (!success) updateLog("CRITICAL: All keys in rotation failed. Check API limits or connection.")
    }

    private fun updateLog(msg: String) {
        _uiLog.value += "\n\n$msg"
    }
}