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

    // --- PERSISTENT DRAFT STATE (Survives Minimize) ---
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
        if (savedKeys.isEmpty()) throw IllegalStateException("Key Vault Empty!")
        val key = savedKeys[currentKeyIndex % savedKeys.size].key
        currentKeyIndex++
        return GenerativeModel(
            modelName = "gemini-3-flash-preview",
            apiKey = key,
            requestOptions = RequestOptions(apiVersion = "v1beta")
        )
    }

    // UPDATED: Now fully agnostic. Uses blueprints from your Database.
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

        updateLog(">> Initializing $platform Engine Blueprint...")

        val personaStyle = personaInstructions ?: "Standard viral strategist."
        val finalEngineRules = engineInstructions ?: "Analyze for social media virality."
        val finalTemplate = engineTemplate ?: "###ARCHITECT_DRAFT###\nC1: [Option 1]\n###END###"

        updateLog(">> Reading pixels for ${personaName ?: "Unknown Persona"}...")
        val videoBytes = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(videoUri)?.use { it.readBytes() }
        }

        runWithRotation { model ->
            model.generateContent(content {
                videoBytes?.let { blob("video/mp4", it) }
                text("""
                    ACT AS: $personaStyle
                    
                    TASK: Analyze pixels and fulfill the $platform strategy.
                    
                    ENGINE BLUEPRINT RULES:
                    $finalEngineRules
                    
                    USER CONTEXT: $contextInfo
                    
                    YOU MUST PROVIDE THE DATA IN THIS EXACT FORMAT:
                    $finalTemplate
                """.trimIndent())
            })
        }
    }

    suspend fun chat(message: String) {
        updateLog(">> Consulting Architect...")
        runWithRotation { model -> model.generateContent(message) }
    }

    fun clearLog() {
        _uiLog.value = "[SYSTEM]: Architect Ready."
        _captionOptions.value = listOf("", "", "")
        _overlayText.value = ""
        _musicTip.value = ""
        _seoTags.value = ""
    }

    private suspend fun runWithRotation(block: suspend (GenerativeModel) -> com.google.ai.client.generativeai.type.GenerateContentResponse) {
        var success = false
        var attempt = 0
        val savedKeys = dao.getAllKeys().first()
        while (attempt < savedKeys.size && !success) {
            try {
                val model = getNextModel()
                val response = withContext(Dispatchers.IO) { block(model) }
                response.text?.let { raw ->
                    updateLog(">> Data Stream Received. Decoding...")
                    _uiLog.value += "\n\n$raw"
                    if (raw.contains("###ARCHITECT_DRAFT###")) parseDraft(raw)
                    success = true
                }
            } catch (e: Exception) {
                attempt++
                updateLog(">> Key Rotation Node failure...")
                delay(500)
            }
        }
    }

    private fun parseDraft(raw: String) {
        val draft = raw.substringAfter("###ARCHITECT_DRAFT###").substringBefore("###END###")
        val lines = draft.trim().lines()
        var hts = ""
        var tgs = ""

        lines.forEach {
            if (it.startsWith("HT:")) hts = it.removePrefix("HT:").trim()
            if (it.startsWith("TAGS:")) tgs = it.removePrefix("TAGS:").trim()
        }

        val caps = mutableListOf("", "", "")
        lines.forEach { line ->
            when {
                line.startsWith("C1:") -> caps[0] = "${line.removePrefix("C1:").trim()}\n\n$hts".trim()
                line.startsWith("C2:") -> caps[1] = "${line.removePrefix("C2:").trim()}\n\n$hts".trim()
                line.startsWith("C3:") -> caps[2] = "${line.removePrefix("C3:").trim()}\n\n$hts".trim()
            }
        }

        _captionOptions.value = caps
        _overlayText.value = lines.find { it.startsWith("OV:") }?.removePrefix("OV:")?.trim() ?: ""
        _musicTip.value = lines.find { it.startsWith("MU:") }?.removePrefix("MU:")?.trim() ?: ""
        _seoTags.value = tgs
    }

    private fun updateLog(msg: String) { _uiLog.value += "\n$msg" }
}