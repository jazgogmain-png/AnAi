package com.anai.app

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class MediaManager(private val context: Context) {
    val player = ExoPlayer.Builder(context).build()

    // Telemetry for the Matrix HUD
    var lastFrameCount = 0

    fun loadVideo(uri: Uri) {
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    /**
     * THE SQUEEZER: Dynamic 1FPS logic.
     * Extracts a cinematic storyboard to keep AI analysis fast and under API limits.
     */
    suspend fun squeezeForAI(
        uri: Uri,
        onProgress: (Int, Int) -> Unit
    ): ByteArray? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val stream = ByteArrayOutputStream()
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L

            // GOLDILOCKS ZONE: 1 frame per second, capped at 100 frames (~6MB total)
            lastFrameCount = (durationMs / 1000).toInt().coerceIn(3, 100)

            val totalUs = durationMs * 1000
            val intervalUs = if (lastFrameCount > 1) totalUs / (lastFrameCount - 1) else 0L

            for (i in 0 until lastFrameCount) {
                val timeUs = i * intervalUs

                // Report progress to the Matrix
                onProgress(i + 1, lastFrameCount)

                val bitmap = try {
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } catch (e: Exception) {
                    null
                }

                bitmap?.let {
                    // Downscale to 480p (Perfect for G3's vision)
                    val scaled = Bitmap.createScaledBitmap(it, 480, 854, false)
                    // 75% quality JPEGs balance detail and speed
                    scaled.compress(Bitmap.CompressFormat.JPEG, 75, stream)
                    it.recycle() // Immediate memory cleanup
                }
            }
            stream.toByteArray()
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) { /* Already released */ }
        }
    }

    fun release() {
        player.release()
    }
}