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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class GeminiManager(
    private val context: Context,
    private val mediaManager: MediaManager,
    private val dao: ArchitectDao
) {
    private var currentKeyIndex = 0
    private val _uiLog = MutableStateFlow("[SYSTEM]: Architect Manager Active.")
    val uiLog = _uiLog.asStateFlow()

    private suspend fun getNextModel(): GenerativeModel {
        val savedKeys = dao.getAllKeys().first()
        if (savedKeys.isEmpty()) throw IllegalStateException("Key Vault is Empty!")

        val key = savedKeys[currentKeyIndex % savedKeys.size].key
        currentKeyIndex++

        return GenerativeModel(
            modelName = "gemini-3-flash-preview",
            apiKey = key,
            requestOptions = RequestOptions(apiVersion = "v1beta")
        )
    }

    suspend fun analyzeVideo(contextInfo: String, platform: String, personaName: String?, personaInstructions: String?, videoUriString: String?) {
        if (videoUriString == null) return
        val videoUri = Uri.parse(videoUriString)

        // --- PLATFORM ENGINE: TIKTOK VIRAL PRO ---
        val platformStyle = when(platform) {
            "TikTok" -> """
                PLATFORM RULES: TikTok Viral Pro Style.
                Structure the output as follows:
                1. VIDEO OVERLAY: A short, high-retention text hook to put on the video screen.
                2. CAPTION: 
                   - LINE 1: Aggressive psychological hook.
                   - LINE 2-3: The main content/joke.
                   - LINE 4: CTA (e.g., 'Double tap for Lola').
                3. HASHTAGS: Use '2-2-1' (2 Broad, 2 Niche, 1 Personal/Trending).
                4. MUSIC/VIBE: Suggest a trending audio style or specific sound effect.
            """.trimIndent()
            "YouTube" -> "Focus on SEO Titles (under 70 chars) and first 2 lines of description for search ranking."
            "Instagram" -> "Focus on aesthetic 'Save-able' value and community engagement CTAs."
            else -> "General Strategy."
        }

        val personaStyle = personaInstructions ?: "Act as a viral strategist."

        updateLog("Executing Pixel Scan | Persona: ${personaName ?: "Standard"}")

        val videoBytes = context.contentResolver.openInputStream(videoUri)?.use { it.readBytes() }

        runWithRotation { model ->
            model.generateContent(content {
                videoBytes?.let { blob("video/mp4", it) }
                text("""
                    ACT AS: $personaStyle
                    
                    $platformStyle
                    
                    USER CONTEXT: $contextInfo
                    
                    TASK: Analyze pixels and return the strategy block. Keep the voice 100% consistent with the Persona instructions.
                """.trimIndent())
            })
        }
    }

    suspend fun analyzeStats(screenshotUri: Uri, userNotes: String) {
        updateLog("Analyzing Performance Screenshot...")
        val imageBytes = context.contentResolver.openInputStream(screenshotUri)?.use { it.readBytes() }
        runWithRotation { model ->
            model.generateContent(content {
                imageBytes?.let { blob("image/png", it) }
                text("Analyze stats professionally. Notes: $userNotes")
            })
        }
    }

    suspend fun chat(message: String) {
        updateLog("Consulting Architect: $message")
        runWithRotation { model -> model.generateContent(message) }
    }

    private suspend fun runWithRotation(block: suspend (GenerativeModel) -> com.google.ai.client.generativeai.type.GenerateContentResponse) {
        var success = false
        var attempt = 0
        try {
            val keysCount = dao.getAllKeys().first().size
            while (attempt < keysCount && !success) {
                try {
                    val model = getNextModel()
                    val response = withContext(Dispatchers.IO) { block(model) }
                    response.text?.let {
                        updateLog("ARCHITECT RESPONSE:\n$it")
                        success = true
                    }
                } catch (e: Exception) {
                    attempt++
                    updateLog("Key failure. Rotating...")
                    delay(500)
                }
            }
        } catch (e: Exception) {
            updateLog("ERROR: ${e.message}")
        }
    }

    private fun updateLog(msg: String) { _uiLog.value += "\n\n$msg" }
}