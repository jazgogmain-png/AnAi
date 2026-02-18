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

        // --- REFINED TIKTOK ENGINE ---
        val platformStyle = when(platform) {
            "TikTok" -> """
                Style: Viral TikTok Pro.
                STRICT HASHTAG BLACKLIST: Do NOT use generic tags like #fyp, #viral, #foryou, or #explore. These cause shadow-bans.
                STRICT 2-2-1 HASHTAG RULE: Provide exactly 5 HIGH-SIGNAL hashtags:
                - 2 Broad (e.g., #pinoycars, #filipinocooking)
                - 2 Niche (Specific to the video actions, e.g., #adoborecipe, #driftcarbuild)
                - 1 Trending/Personal (Relevant to current Pinoy trends or channel brand).
            """.trimIndent()
            "YouTube" -> "Style: YouTube Shorts. High-engagement hooks and SEO-rich titles."
            "Instagram" -> "Style: Aesthetic IG Reel. Focus on community CTAs and visual quality."
            else -> "General Strategy."
        }

        val personaStyle = personaInstructions ?: "Viral strategist."

        updateLog(">> Architect is reading video data. Hold tight...")
        val videoBytes = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(videoUri)?.use { it.readBytes() }
        }

        updateLog(">> Pixels extracted. Consulting G3 $platform Engine...")

        runWithRotation { model ->
            model.generateContent(content {
                videoBytes?.let { blob("video/mp4", it) }
                text("""
                    ACT AS: $personaStyle
                    
                    CRITICAL VOICE NOTE: Do not let the platform formatting weaken your persona. 
                    Maintain the full Taglish Conyo and Brainrot slang energy in every caption option.
                    
                    PLATFORM RULES:
                    $platformStyle
                    
                    USER CONTEXT: $contextInfo
                    
                    TASK: Analyze video pixels. 
                    FIRST: Give your high-energy commentary in character.
                    SECOND: You MUST provide the final data in this exact block:
                    
                    ###ARCHITECT_DRAFT###
                    C1: [Full Persona Caption 1 + 5 High-Signal Hashtags]
                    C2: [Full Persona Caption 2 + 5 High-Signal Hashtags]
                    C3: [Full Persona Caption 3 + 5 High-Signal Hashtags]
                    OV: [Video Overlay Text]
                    MU: [Music Recommendation]
                    ###END###
                """.trimIndent())
            })
        }
    }

    suspend fun analyzeStats(screenshotUri: Uri, userNotes: String) {
        updateLog(">> Analyzing Performance Data...")
        val imageBytes = context.contentResolver.openInputStream(screenshotUri)?.use { it.readBytes() }
        runWithRotation { model ->
            model.generateContent(content {
                imageBytes?.let { blob("image/png", it) }
                text("Analyze stats professionally. Notes: $userNotes")
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
            val keysCount = savedKeys.size

            while (attempt < keysCount && !success) {
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
                    updateLog(">> Rotation Active: Node failure. Switching keys...")
                    delay(500)
                }
            }
        } catch (e: Exception) {
            updateLog(">> CRITICAL ERROR: ${e.message}")
        }
    }

    private fun updateLog(msg: String) {
        _uiLog.value += "\n$msg"
    }
}