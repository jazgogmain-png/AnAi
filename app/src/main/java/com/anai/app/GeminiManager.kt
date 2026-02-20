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

    // PERSONA FORGE
    suspend fun forgePersona(description: String): String {
        _isProcessing.value = true
        var result = ""
        runWithRotation { model ->
            val response = model.generateContent("Act as a prompt engineer. Turn this into a high-performance persona instructional sheet: $description")
            result = response.text ?: "Forge Failed"
            response
        }
        _isProcessing.value = false
        return result
    }

    suspend fun analyzeVideo(contextInfo: String, platform: String, personaName: String?, personaInstructions: String?, videoUriString: String?, engineInstructions: String?, engineTemplate: String?) {
        if (videoUriString == null) return
        val videoUri = Uri.parse(videoUriString)
        _isProcessing.value = true
        val videoBytes = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(videoUri)?.use { it.readBytes() }
        }
        runWithRotation { model ->
            model.generateContent(content {
                videoBytes?.let { blob("video/mp4", it) }
                text("SOUL: $personaInstructions\nENGINE: $engineInstructions\nPLATFORM: $platform\nCONTEXT: $contextInfo\nFORMAT: $engineTemplate")
            })
        }
        _isProcessing.value = false
    }

    suspend fun extractPrompt(videoUriString: String?): String {
        if (videoUriString == null) return "No Video"
        _isProcessing.value = true
        val videoUri = Uri.parse(videoUriString)
        val videoBytes = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(videoUri)?.use { it.readBytes() } }
        var result = ""
        runWithRotation { model ->
            val response = model.generateContent(content {
                videoBytes?.let { blob("video/mp4", it) }
                text("[TASK]: Reverse-engineer this video into a high-fidelity Veo prompt.")
            })
            result = response.text ?: ""
            response
        }
        _isProcessing.value = false
        return result
    }

    // FIXED: THE STUDY CHAT (Now actually logs the response!)
    suspend fun chat(message: String) {
        _isProcessing.value = true
        // Log the user's message first
        updateLog("\n[USER]: $message")

        runWithRotation { model ->
            val response = model.generateContent(message)
            response.text?.let {
                updateLog("\n[ARCHITECT]: $it")
            }
            response
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
                    // For Studio scans, we still show the raw output in the log
                    if (raw.contains("###ARCHITECT_DRAFT###")) {
                        _uiLog.value += "\n\n$raw"
                        parseDraft(raw)
                    }
                    success = true
                }
            } catch (e: Exception) {
                attempt++
                updateLog(">> ROTATING NODE...")
                delay(800)
            }
        }
    }

    private fun parseDraft(raw: String) {
        val draft = raw.substringAfter("###ARCHITECT_DRAFT###").substringBefore("###END###")
        val lines = draft.trim().lines()
        val caps = mutableListOf("", "", "")
        lines.forEach { line ->
            val t = line.trim()
            when {
                t.startsWith("C1:") -> caps[0] = t.removePrefix("C1:").trim()
                t.startsWith("C2:") -> caps[1] = t.removePrefix("C2:").trim()
                t.startsWith("C3:") -> caps[2] = t.removePrefix("C3:").trim()
                t.startsWith("OV:") -> _overlayText.value = t.removePrefix("OV:").trim()
                t.startsWith("MU:") -> _musicTip.value = t.removePrefix("MU:").trim()
                t.startsWith("TAGS:") -> _seoTags.value = t.removePrefix("TAGS:").trim()
                t.startsWith("DESC:") -> _descPart.value = t.removePrefix("DESC:").trim()
                t.startsWith("HT:") -> _hashtagPart.value = t.removePrefix("HT:").trim()
            }
        }
        _captionOptions.value = caps
    }

    fun clearLog() {
        _uiLog.value = "[SYSTEM]: Architect Ready."; _isProcessing.value = false
        _captionOptions.value = listOf("", "", ""); _overlayText.value = ""
        _musicTip.value = ""; _seoTags.value = ""; _descPart.value = ""; _hashtagPart.value = ""
    }

    private fun updateLog(msg: String) {
        _uiLog.value += msg
    }
}