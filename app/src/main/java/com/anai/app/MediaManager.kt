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
import java.io.File
import java.io.FileOutputStream

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
     * SNATCH THUMBNAIL:
     * Saves the first frame to internal storage so the Vault stays visual forever.
     */
    suspend fun snatchThumbnail(uri: Uri): String? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

            bitmap?.let {
                val fileName = "thumb_${System.currentTimeMillis()}.jpg"
                val file = File(context.filesDir, fileName)
                FileOutputStream(file).use { out ->
                    it.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                it.recycle()
                file.absolutePath // This is the URI we save to the BlueprintEntity
            }
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    /**
     * THE SQUEEZER (150 PROTOCOL):
     * Extracts a high-density storyboard for precision "Golden Hook" detection.
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

            lastFrameCount = 150
            val totalUs = durationMs * 1000
            val intervalUs = if (lastFrameCount > 1) totalUs / (lastFrameCount - 1) else 0L

            for (i in 0 until lastFrameCount) {
                val timeUs = i * intervalUs
                onProgress(i + 1, lastFrameCount)

                val bitmap = try {
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } catch (e: Exception) {
                    null
                }

                bitmap?.let {
                    val scaled = Bitmap.createScaledBitmap(it, 480, 854, false)
                    scaled.compress(Bitmap.CompressFormat.JPEG, 75, stream)
                    it.recycle()
                }
            }
            stream.toByteArray()
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) { }
        }
    }

    fun release() {
        player.release()
    }
}