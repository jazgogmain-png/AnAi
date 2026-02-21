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
    private val _seoTags = MutableStateFlow("")
    val seoTags = _seoTags.asStateFlow()
    private val _descPart = MutableStateFlow("")
    val descPart = _descPart.asStateFlow()
    private val _hashtagPart = MutableStateFlow("")
    val hashtagPart = _hashtagPart.asStateFlow()

    private val _hookPart = MutableStateFlow("")
    val hookPart = _hookPart.asStateFlow()

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

        updateLog(">> BOOTING ANALYZER...")
        updateLog(">> TARGET: $platform STUDIO")
        updateLog(">> SOUL: ${personaName ?: "Default"}")

        val squeezedBytes = withContext(Dispatchers.IO) {
            updateLog(">> INITIATING SQUEEZE PROTOCOL (150 NODES)...")
            val result = mediaManager.squeezeForAI(videoUri) { current, total ->
                if (current % 10 == 0 || current == total) {
                    updateLog(">> CRUNCHING: $current/$total NODES SNATCHED...")
                }
            }
            updateLog(">> SQUEEZE COMPLETE: ${mediaManager.lastFrameCount} NODES | ${result?.size ?: 0} BYTES")
            result
        }

        updateLog(">> HANDSHAKING WITH G3 NODES...")

        runWithRotation { model ->
            updateLog(">> NODE ACTIVE. UPLOADING STORYBOARD...")
            val response = model.generateContent(content {
                squeezedBytes?.let { blob("image/jpeg", it) }
                text("""
                    [SYSTEM ROLE]: You are the ANAI Architect.
                    
                    [SOUL DNA - MANDATORY]: 
                    $personaInstructions
                    
                    [ENGINE LOGIC]: 
                    $engineInstructions
                    
                    [STRICT OUTPUT RULES]:
                    1. Identify the 'GOLDEN HOOK' timestamp based on the 150 frames provided.
                    2. The 'DESC' field MUST contain the STATIC BIO provided in the [SOUL DNA].
                    3. Start the 'DESC' with one unique sentence based on video analysis.
                    
                    [FORMAT TEMPLATE]: 
                    $engineTemplate
                """.trimIndent())
            })
            updateLog(">> RESPONSE RECEIVED. PARSING DATA...")
            response
        }

        updateLog(">> BLUEPRINT FINALIZED.")
        _isProcessing.value = false
    }

    suspend fun chat(message: String) {
        _isProcessing.value = true
        updateLog("\n[USER]: $message")
        runWithRotation { model ->
            val response = model.generateContent(message)
            response.text?.let { updateLog("\n[ARCHITECT]: $it") }
            response
        }
        _isProcessing.value = false
    }

    suspend fun forgePersona(description: String): String {
        _isProcessing.value = true
        updateLog(">> FORGING NEW SOUL DNA...")
        var result = ""
        runWithRotation { model ->
            val response = model.generateContent("Turn this into a structured Soul DNA sheet with sections for BIO_ANCHOR, STATIC_HT, STATIC_TAGS and TONE: $description")
            result = response.text ?: "Forge Failed"
            updateLog(">> SOUL CRYSTALLIZED.")
            response
        }
        _isProcessing.value = false
        return result
    }

    // UPDATED: High-Fidelity Hook-Tuned Prompt Extraction
    suspend fun extractPrompt(videoUriString: String?): String {
        if (videoUriString == null) return "No Video"
        val videoUri = Uri.parse(videoUriString)
        _isProcessing.value = true
        updateLog(">> INITIATING VEO REVERSE-ENGINEERING...")

        // Using Squeezer instead of raw bytes: Much faster, still enough detail for a prompt
        val squeezedBytes = withContext(Dispatchers.IO) {
            mediaManager.squeezeForAI(videoUri) { _, _ -> } // Silent progress
        }

        var result = ""
        runWithRotation { model ->
            val response = model.generateContent(content {
                squeezedBytes?.let { blob("image/jpeg", it) }
                text("""
                    [TASK]: Reverse-engineer this storyboard into a high-fidelity Veo prompt.
                    [STRICT RULE]: Identify the 'GOLDEN HOOK' (the single most visually impactful moment).
                    [FORMAT]: The prompt must explicitly instruction the AI generator to start with or peak at that GOLDEN HOOK moment within the first 1.5 seconds.
                """.trimIndent())
            })
            result = response.text ?: ""
            updateLog(">> HOOK-TUNED PROMPT EXTRACTED.")
            response
        }
        _isProcessing.value = false
        return result
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
                        parseDraft(raw)
                    } else if (_isProcessing.value && !raw.contains("[ARCHITECT]")) {
                        // Regular logging handled elsewhere
                    }
                    success = true
                }
            } catch (e: Exception) {
                attempt++
                updateLog(">> ROTATING NODE (ATTEMPT $attempt)...")
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
                t.startsWith("TAGS:") -> _seoTags.value = t.removePrefix("TAGS:").trim()
                t.startsWith("HT:") -> _hashtagPart.value = t.removePrefix("HT:").trim()
                t.startsWith("DESC:") -> _descPart.value = t.removePrefix("DESC:").trim()
                t.startsWith("HOOK:") -> _hookPart.value = t.removePrefix("HOOK:").trim()
                t.startsWith("BIO_ANCHOR:") -> {
                    _descPart.value += "\n\n" + t.removePrefix("BIO_ANCHOR:").trim()
                }
            }
        }
        _captionOptions.value = caps
    }

    fun clearLog() {
        _uiLog.value = "[SYSTEM]: Architect Ready."; _isProcessing.value = false
        _captionOptions.value = listOf("", "", ""); _seoTags.value = ""
        _descPart.value = ""; _hashtagPart.value = ""; _hookPart.value = ""
    }

    private fun updateLog(msg: String) { _uiLog.value += "\n$msg" }
}