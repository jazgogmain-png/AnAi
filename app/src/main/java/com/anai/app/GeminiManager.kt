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
    private val _uiLog = MutableStateFlow("[SYSTEM]: Architect Active.")
    val uiLog = _uiLog.asStateFlow()

    private suspend fun getNextModel(): GenerativeModel {
        val savedKeys = dao.getAllKeys().first()
        if (savedKeys.isEmpty()) throw IllegalStateException("Key Vault Empty!")
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

        updateLog(">> Initializing Pixel Scan for $platform...")

        val platformStyle = when(platform) {
            "TikTok" -> """
                Style: TikTok Viral "Group Chat" Mode. All lowercase. 5-12 words.
                Formula: [reaction] + [disbelief] + [emoji].
                Hashtags: Exactly 5 high-signal tags using 2-2-1 strategy. NO generic tags.
            """.trimIndent()
            "Instagram" -> """
                Style: Aesthetic IG Reels. Short hooks.
                Hashtags: Exactly 5 niche-specific tags. 
                Tone: Engaging and visual-focused.
            """.trimIndent()
            "YouTube" -> "Style: YouTube Shorts. High-engagement hooks and SEO titles."
            else -> "General Strategy."
        }

        val personaStyle = personaInstructions ?: "Viral strategist."

        updateLog(">> Architect is reading video data...")
        val videoBytes = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(videoUri)?.use { it.readBytes() }
        }

        updateLog(">> Pixels extracted. Consulting G3 $platform Engine...")

        runWithRotation { model ->
            model.generateContent(content {
                videoBytes?.let { blob("video/mp4", it) }
                text("""
                    ACT AS: $personaStyle
                    
                    CRITICAL: For TikTok/IG, use "Group Chat Style" (raw, lowercase, no description).
                    
                    PLATFORM RULES:
                    $platformStyle
                    
                    USER CONTEXT: $contextInfo
                    
                    TASK: Analyze video pixels and provide strategy.
                    
                    ###ARCHITECT_DRAFT###
                    C1: [Shock Archetype]
                    C2: [Relatable Archetype]
                    C3: [Chaos Archetype]
                    OV: [Overlay Text]
                    MU: [Music Tip]
                    HT: [5 Hashtags]
                    ###END###
                """.trimIndent())
            })
        }
    }

    suspend fun chat(message: String) {
        updateLog(">> Sending message to Study...")
        runWithRotation { model -> model.generateContent(message) }
    }

    private suspend fun runWithRotation(block: suspend (GenerativeModel) -> com.google.ai.client.generativeai.type.GenerateContentResponse) {
        var success = false
        var attempt = 0
        try {
            val savedKeys = dao.getAllKeys().first()
            while (attempt < savedKeys.size && !success) {
                try {
                    val model = getNextModel()
                    updateLog(">> Node ${currentKeyIndex}: Connection Established...")
                    val response = withContext(Dispatchers.IO) { block(model) }
                    response.text?.let {
                        updateLog(">> Data Stream Received. Decoding...")
                        _uiLog.value += "\n\n$it"
                        success = true
                    }
                } catch (e: Exception) {
                    attempt++
                    updateLog(">> Rotation Active: Node failure...")
                    delay(500)
                }
            }
        } catch (e: Exception) {
            updateLog(">> ERROR: ${e.message}")
        }
    }

    private fun updateLog(msg: String) { _uiLog.value += "\n$msg" }
}