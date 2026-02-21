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

    // Public variable for the Matrix HUD telemetry
    var lastFrameCount = 0

    fun loadVideo(uri: Uri) {
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    /**
     * THE SQUEEZER (150 PROTOCOL):
     * Extracts a high-density storyboard for precision "Golden Hook" detection.
     * Stays under 20MB while providing nearly 18fps for short videos.
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

            // STATIC 150: High-fidelity vision across all video lengths
            lastFrameCount = 150

            val totalUs = durationMs * 1000
            val intervalUs = if (lastFrameCount > 1) totalUs / (lastFrameCount - 1) else 0L

            for (i in 0 until lastFrameCount) {
                val timeUs = i * intervalUs

                // Report real-time telemetry back to GeminiManager
                onProgress(i + 1, lastFrameCount)

                val bitmap = try {
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } catch (e: Exception) {
                    null
                }

                bitmap?.let {
                    // Downscale to 480p (Portrait: 480x854)
                    val scaled = Bitmap.createScaledBitmap(it, 480, 854, false)
                    // 75% quality JPEGs balance visual soul and upload speed
                    scaled.compress(Bitmap.CompressFormat.JPEG, 75, stream)
                    it.recycle() // Critical for avoiding OutOfMemory on 150 frames
                }
            }
            stream.toByteArray()
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) { /* Handled */ }
        }
    }

    fun release() {
        player.release()
    }
}