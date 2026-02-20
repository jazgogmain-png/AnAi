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
    private val _uiLog = MutableStateFlow("[SYSTEM]: Architect G3 Online.")
    val uiLog = _uiLog.asStateFlow()
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    // UI Output States
    private val _captionOptions = MutableStateFlow(listOf("", "", ""))
    val captionOptions = _captionOptions.asStateFlow()
    private val _overlayText = MutableStateFlow("")
    val overlayText = _overlayText.asStateFlow()
    private val _musicTip = MutableStateFlow("")
    val musicTip = _musicTip.asStateFlow()
    private val _seoTags = MutableStateFlow("")
    val seoTags = _seoTags.asStateFlow()
    private val _descPart = MutableStateFlow("")
    val descPart = _descPart.asStateFlow()
    private val _hashtagPart = MutableStateFlow("")
    val hashtagPart = _hashtagPart.asStateFlow()

    private suspend fun getNextModel(): GenerativeModel {
        val savedKeys = dao.getAllKeys().first()
        if (savedKeys.isEmpty()) throw IllegalStateException("Vault Empty")
        val key = savedKeys[currentKeyIndex % savedKeys.size].key
        currentKeyIndex++
        return GenerativeModel(
            modelName = "gemini-3-flash-preview",
            apiKey = key,
            requestOptions = RequestOptions(apiVersion = "v1beta")
        )
    }

    suspend fun analyzeVideo(
        contextInfo: String,
        platform: String,
        personaName: String?,
        personaInstructions: String?,
        videoUriString: String?,
        engineInstructions: String?,
        engineTemplate: String?
    ) {
        if (videoUriString == null) return
        val videoUri = Uri.parse(videoUriString)
        _isProcessing.value = true

        val videoBytes = withContext(Dispatchers.IO) {
            updateLog(">> EXTRACTING PIXELS FOR $platform...")
            context.contentResolver.openInputStream(videoUri)?.use { it.readBytes() }
        }

        runWithRotation { model ->
            model.generateContent(content {
                videoBytes?.let { blob("video/mp4", it) }
                text("""
                    SOUL (VOICE): $personaInstructions
                    ENGINE (STRUCTURE): $engineInstructions
                    PLATFORM: $platform
                    CONTEXT: $contextInfo
                    FORMAT: $engineTemplate
                """.trimIndent())
            })
        }
        _isProcessing.value = false
    }

    // NEW: PROMPT LAB EXTRACTION (VEO ENGINE)
    suspend fun extractPrompt(videoUriString: String?): String {
        if (videoUriString == null) return "No Video Loaded"
        val videoUri = Uri.parse(videoUriString)
        _isProcessing.value = true
        updateLog(">> EXTRACTING VISUAL DNA...")

        val videoBytes = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(videoUri)?.use { it.readBytes() }
        }

        var result = ""
        runWithRotation { model ->
            val response = model.generateContent(content {
                videoBytes?.let { blob("video/mp4", it) }
                text("""
                    [TASK]: Reverse-engineer this video into a high-fidelity AI video prompt for Google Veo.
                    [FOCUS]: Lighting, camera movement, lens type, color grading, and textures.
                    [FORMAT]: Output ONLY the prompt. No intro or outro.
                """.trimIndent())
            })
            result = response.text ?: "Extraction Failed"
            response
        }
        _isProcessing.value = false
        return result
    }

    // NEW: THE STUDY CHAT SYSTEM
    suspend fun chat(message: String) {
        _isProcessing.value = true
        updateLog(">> STUDY QUERY: $message")
        runWithRotation { model ->
            model.generateContent(message)
        }
        _isProcessing.value = false
    }

    private suspend fun runWithRotation(block: suspend (GenerativeModel) -> com.google.ai.client.generativeai.type.GenerateContentResponse) {
        var success = false
        var attempt = 0
        val savedKeys = dao.getAllKeys().first()
        while (attempt < savedKeys.size && !success) {
            try {
                val response = withContext(Dispatchers.IO) { block(getNextModel()) }
                response.text?.let { raw ->
                    _uiLog.value += "\n\n$raw"
                    if (raw.contains("###ARCHITECT_DRAFT###")) parseDraft(raw)
                    success = true
                }
            } catch (e: Exception) {
                attempt++
                updateLog(">> ROTATING NODE (Attempt $attempt)...")
                delay(800)
            }
        }
    }

    private fun parseDraft(raw: String) {
        val draft = raw.substringAfter("###ARCHITECT_DRAFT###").substringBefore("###END###")
        val lines = draft.trim().lines()
        val caps = mutableListOf("", "", "")

        lines.forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("C1:") -> caps[0] = trimmed.removePrefix("C1:").trim()
                trimmed.startsWith("C2:") -> caps[1] = trimmed.removePrefix("C2:").trim()
                trimmed.startsWith("C3:") -> caps[2] = trimmed.removePrefix("C3:").trim()
                trimmed.startsWith("OV:") -> _overlayText.value = trimmed.removePrefix("OV:").trim()
                trimmed.startsWith("MU:") -> _musicTip.value = trimmed.removePrefix("MU:").trim()
                trimmed.startsWith("TAGS:") -> _seoTags.value = trimmed.removePrefix("TAGS:").trim()
                trimmed.startsWith("DESC:") -> _descPart.value = trimmed.removePrefix("DESC:").trim()
                trimmed.startsWith("HT:") -> _hashtagPart.value = trimmed.removePrefix("HT:").trim()
            }
        }
        _captionOptions.value = caps
    }

    fun clearLog() {
        _uiLog.value = "[SYSTEM]: Architect Ready."; _isProcessing.value = false
        _captionOptions.value = listOf("", "", ""); _overlayText.value = ""
        _musicTip.value = ""; _seoTags.value = ""; _descPart.value = ""; _hashtagPart.value = ""
    }

    private fun updateLog(msg: String) { _uiLog.value += "\n$msg" }
}