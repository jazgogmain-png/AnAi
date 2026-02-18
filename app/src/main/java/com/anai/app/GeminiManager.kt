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

    private val _captionOptions = MutableStateFlow(listOf("", "", ""))
    val captionOptions = _captionOptions.asStateFlow()

    private val _overlayText = MutableStateFlow("")
    val overlayText = _overlayText.asStateFlow()

    private val _musicTip = MutableStateFlow("")
    val musicTip = _musicTip.asStateFlow()

    private val _seoTags = MutableStateFlow("")
    val seoTags = _seoTags.asStateFlow()

    private suspend fun getNextModel(): GenerativeModel {
        val savedKeys = dao.getAllKeys().first()
        if (savedKeys.isEmpty()) throw IllegalStateException("Vault Empty")

        val keyIndex = currentKeyIndex % savedKeys.size
        val key = savedKeys[keyIndex].key
        currentKeyIndex++

        // LASER PRECISION: G3 Flash Preview Only
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

        updateLog(">> BOOTING G3 PIXEL ENGINE...")
        updateLog(">> INJECTING SOUL: ${personaName ?: "Unknown"}")

        val videoBytes = withContext(Dispatchers.IO) {
            updateLog(">> SHREDDING BYTES FOR G3 UPLOAD...")
            context.contentResolver.openInputStream(videoUri)?.use { it.readBytes() }
        }

        runWithRotation { model ->
            updateLog(">> HANDSHAKE WITH G3 FLASH PREVIEW...")
            model.generateContent(content {
                videoBytes?.let { blob("video/mp4", it) }
                text("""
                    [SYSTEM ROLE]: $personaInstructions
                    [STRUCTURAL RULES]: $engineInstructions
                    [USER CONTEXT]: $contextInfo
                    [REQUIRED OUTPUT FORMAT]: $engineTemplate
                """.trimIndent())
            })
        }
        _isProcessing.value = false
    }

    suspend fun chat(message: String) {
        _isProcessing.value = true
        updateLog(">> QUERY: $message")
        runWithRotation { model -> model.generateContent(message) }
        _isProcessing.value = false
    }

    fun clearLog() {
        _uiLog.value = "[SYSTEM]: Architect Ready."
        _captionOptions.value = listOf("", "", "")
        _overlayText.value = ""
        _musicTip.value = ""
        _seoTags.value = ""
        _isProcessing.value = false
    }

    private suspend fun runWithRotation(block: suspend (GenerativeModel) -> com.google.ai.client.generativeai.type.GenerateContentResponse) {
        var success = false
        var attempt = 0
        val savedKeys = dao.getAllKeys().first()

        if (savedKeys.isEmpty()) {
            updateLog(">> CRITICAL: NO KEYS FOUND.")
            return
        }

        while (attempt < savedKeys.size && !success) {
            try {
                val model = getNextModel()
                val keyNum = (currentKeyIndex - 1) % savedKeys.size
                updateLog(">> TRYING NODE $keyNum...")

                val response = withContext(Dispatchers.IO) { block(model) }

                response.text?.let { raw ->
                    updateLog(">> STREAM CAPTURED. PARSING...")
                    _uiLog.value += "\n\n$raw"
                    if (raw.contains("###ARCHITECT_DRAFT###")) {
                        parseDraft(raw)
                        updateLog(">> BLUEPRINT DRAFT SYNCED.")
                    }
                    success = true
                }
            } catch (e: Exception) {
                attempt++
                val errorMsg = e.message ?: ""
                val errorType = when {
                    errorMsg.contains("429") -> "QUOTA_FULL"
                    errorMsg.contains("503") -> "G3_OVERLOAD"
                    else -> "UNSTABLE_NODE"
                }
                updateLog(">> NODE $attempt ERROR: $errorType")
                if (attempt < savedKeys.size) {
                    updateLog(">> ROTATING IN 1.2s...")
                    delay(1200)
                }
            }
        }
    }

    private fun parseDraft(raw: String) {
        val draft = raw.substringAfter("###ARCHITECT_DRAFT###").substringBefore("###END###")
        val lines = draft.trim().lines()
        val caps = mutableListOf("", "", "")

        // Find hashtags first to append to captions
        val hts = lines.find { it.startsWith("HT:") }?.removePrefix("HT:")?.trim() ?: ""

        lines.forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("C1:") -> caps[0] = "${trimmed.removePrefix("C1:").trim()}\n\n$hts"
                trimmed.startsWith("C2:") -> caps[1] = "${trimmed.removePrefix("C2:").trim()}\n\n$hts"
                trimmed.startsWith("C3:") -> caps[2] = "${trimmed.removePrefix("C3:").trim()}\n\n$hts"
                trimmed.startsWith("OV:") -> _overlayText.value = trimmed.removePrefix("OV:").trim()
                trimmed.startsWith("MU:") -> _musicTip.value = trimmed.removePrefix("MU:").trim()
                trimmed.startsWith("TAGS:") -> _seoTags.value = trimmed.removePrefix("TAGS:").trim()
            }
        }
        _captionOptions.value = caps
    }

    private fun updateLog(msg: String) {
        _uiLog.value += "\n$msg"
    }
}