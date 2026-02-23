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

    // 📟 THE TERMINAL ENGINE
    private val _statusLogs = MutableStateFlow<List<String>>(emptyList())
    val statusLogs = _statusLogs.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _captionOptions = MutableStateFlow(listOf("", "", ""))
    val captionOptions = _captionOptions.asStateFlow()
    private val _seoTags = MutableStateFlow("")
    val seoTags = _seoTags.asStateFlow()
    private val _descPart = MutableStateFlow("")
    val descPart = _descPart.asStateFlow()
    private val _hashtagPart = MutableStateFlow("")
    val hashtagPart = _hashtagPart.asStateFlow()

    private val _hookPart = MutableStateFlow("")
    val hookPart = _hookPart.asStateFlow()
    private val _auraPart = MutableStateFlow("")
    val auraPart = _auraPart.asStateFlow()

    // 🤖 HIGH-OUTPUT LOGGING
    private fun pushLog(msg: String) {
        val current = _statusLogs.value.toMutableList()
        val timestamp = System.currentTimeMillis() % 10000
        current.add("[$timestamp] $msg")
        _statusLogs.value = current
    }

    private suspend fun getNextModel(): GenerativeModel {
        val savedKeys = dao.getAllKeys().first()
        if (savedKeys.isEmpty()) {
            pushLog("!! CRITICAL: KEY VAULT EMPTY !!")
            throw IllegalStateException("Vault Empty")
        }
        val keyObj = savedKeys[currentKeyIndex % savedKeys.size]
        currentKeyIndex++
        pushLog("ROTATING UPLINK: KEY_ID_#${keyObj.id}")
        return GenerativeModel(
            modelName = "gemini-3-flash-preview",
            apiKey = keyObj.key,
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
        _statusLogs.value = listOf(">> ARCHITECT G3 BOOTING...")
        delay(300)

        pushLog("MOUNTING VIDEO: ${videoUri.lastPathSegment}")
        pushLog("ALIGNED SOUL: ${personaName?.uppercase() ?: "MASTER"}")
        pushLog("LOGIC ENGINE: ${platform.uppercase()}")

        try {
            pushLog("SQUEEZING TEMPORAL DNA (VIDEO BYTES)...")
            val squeezedBytes = withContext(Dispatchers.IO) {
                mediaManager.squeezeForAI(videoUri) { _, _ -> }
            }

            val byteSize = squeezedBytes?.size ?: 0
            pushLog("COMPRESSION COMPLETE: ${(byteSize / 1024)} KB PREPPED.")
            pushLog("ESTABLISHING GEMINI G3 LINK...")

            runWithRotation { model ->
                pushLog("UPLINK SECURE. SCANNING VISUALS...")
                val response = model.generateContent(content {
                    squeezedBytes?.let { blob("image/jpeg", it) }
                    text("""
                        [SYSTEM ROLE]: You are the ANAI Architect.
                        [SOUL DNA]: $personaInstructions
                        [ENGINE LOGIC]: $engineInstructions
                        [STRICT RULES]: 1. NO long descriptions. 2. NO bio anchors for TikTok. 
                        [FORMAT TEMPLATE]: $engineTemplate
                    """.trimIndent())
                })
                pushLog("SCAN SUCCESSFUL. DECODING DRAFT...")
                response
            }
        } catch (e: Exception) {
            pushLog("!! SCAN INTERRUPTED: ${e.message} !!")
        } finally {
            pushLog(">> SCAN DEPLOYED. RETURNING TO STUDIO.")
            _isProcessing.value = false
        }
    }

    suspend fun chat(message: String) {
        _isProcessing.value = true
        runWithRotation { model ->
            val response = model.generateContent(message)
            response.text?.let { updateLog("\n[ARCHITECT]: $it") }
            response
        }
        _isProcessing.value = false
    }

    suspend fun forgePersona(description: String): String {
        _isProcessing.value = true
        var result = ""
        runWithRotation { model ->
            val response = model.generateContent("Turn this into a Soul DNA sheet. Preserve personality: $description")
            result = response.text ?: "Forge Failed"
            response
        }
        _isProcessing.value = false
        return result
    }

    suspend fun extractPrompt(videoUriString: String?): String {
        if (videoUriString == null) return "No Video"
        val videoUri = Uri.parse(videoUriString)
        _isProcessing.value = true
        _statusLogs.value = listOf(">> BOOTING PROMPT LAB...")

        try {
            pushLog("EXTRACTING VISUAL DNA...")
            val squeezedBytes = withContext(Dispatchers.IO) { mediaManager.squeezeForAI(videoUri) { _, _ -> } }
            var result = ""
            runWithRotation { model ->
                pushLog("REVERSE-ENGINEERING CINEMATIC PROMPT...")
                val response = model.generateContent(content {
                    squeezedBytes?.let { blob("image/jpeg", it) }
                    text("[TASK]: Reverse-engineer this into a 300-char Veo prompt. Peak at GOLDEN HOOK within 1.5s.")
                })
                result = response.text ?: ""
                response
            }
            return result
        } finally {
            _isProcessing.value = false
        }
    }

    suspend fun extractPromptWithVibe(videoUriString: String?, referenceAura: String): String {
        if (videoUriString == null) return "No Video"
        val videoUri = Uri.parse(videoUriString)
        _isProcessing.value = true
        pushLog("LAB: INJECTING REFERENCE VIBE...")

        val squeezedBytes = withContext(Dispatchers.IO) { mediaManager.squeezeForAI(videoUri) { _, _ -> } }
        var result = ""
        runWithRotation { model ->
            pushLog("RE-RESTRUCTURING CINEMATIC BLUEPRINT...")
            val response = model.generateContent(content {
                squeezedBytes?.let { blob("image/jpeg", it) }
                text("""
                    [TASK]: Reverse-engineer this storyboard into a Veo prompt.
                    [VIBE INJECTION]: Use this high-performing Aura profile as the blueprint: "$referenceAura"
                    [STRICT RULE]: Max 300 chars. Ensure visual style matches the reference Aura.
                """.trimIndent())
            })
            result = response.text ?: ""
            response
        }
        _isProcessing.value = false
        return result
    }

    fun clearLog() {
        _captionOptions.value = listOf("", "", "")
        _seoTags.value = ""
        _descPart.value = ""
        _hashtagPart.value = ""
        _hookPart.value = ""
        _auraPart.value = ""
        _statusLogs.value = emptyList()
        _uiLog.value = "[SYSTEM]: Architect G3 Online. Ready for Scan."
    }

    private suspend fun runWithRotation(block: suspend (GenerativeModel) -> com.google.ai.client.generativeai.type.GenerateContentResponse) {
        var success = false
        var attempt = 0
        val savedKeys = dao.getAllKeys().first()
        while (attempt < savedKeys.size && !success) {
            try {
                val response = withContext(Dispatchers.IO) { block(getNextModel()) }
                response.text?.let { raw ->
                    if (raw.contains("###ARCHITECT_DRAFT###")) {
                        pushLog("VALIDATING ARCHITECT_DRAFT...")
                        parseDraft(raw)
                        success = true
                    } else {
                        pushLog("?? INVALID DRAFT DETECTED - RETRYING...")
                        attempt++
                    }
                }
            } catch (e: Exception) {
                attempt++
                pushLog("!! UPLINK FAILED (ATTEMPT $attempt): ${e.localizedMessage}")
                delay(800)
            }
        }
        if (!success) pushLog("!! ALL UPLINKS EXHAUSTED !!")
    }

    private fun parseDraft(raw: String) {
        val draft = raw.substringAfter("###ARCHITECT_DRAFT###").substringBefore("###END###")
        val lines = draft.trim().lines()
        val caps = mutableListOf("", "", "")

        pushLog("EXTRACTING VIRAL HOOKS...")
        lines.forEach { line ->
            val t = line.trim()
            when {
                t.startsWith("C1:") -> caps[0] = t.removePrefix("C1:").trim()
                t.startsWith("C2:") -> caps[1] = t.removePrefix("C2:").trim()
                t.startsWith("C3:") -> caps[2] = t.removePrefix("C3:").trim()
                t.startsWith("TAGS:") -> { _seoTags.value = t.removePrefix("TAGS:").trim(); pushLog("TAGS SYNCED.") }
                t.startsWith("HT:") -> _hashtagPart.value = t.removePrefix("HT:").trim()
                t.startsWith("DESC:") -> _descPart.value = t.removePrefix("DESC:").trim()
                t.startsWith("HOOK:") -> { _hookPart.value = t.removePrefix("HOOK:").trim(); pushLog("HOOK CAPTURED.") }
                t.startsWith("AURA:") -> { _auraPart.value = t.removePrefix("AURA:").trim(); pushLog("AURA MAPPED.") }
            }
        }
        _captionOptions.value = caps
    }

    private fun updateLog(msg: String) { _uiLog.value += "\n$msg" }

    private fun pushContextLog(msg: String) {
        val current = _statusLogs.value.toMutableList()
        current.add(msg)
        _statusLogs.value = current
    }
}