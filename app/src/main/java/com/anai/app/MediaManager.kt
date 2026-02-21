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

    fun loadVideo(uri: Uri) {
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    // THE SQUEEZER: Creates a lightweight representation for G3
    suspend fun squeezeForAI(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            // Instead of the whole video, we grab 5 key frames to "squeezer" the data
            // G3 can analyze these perfectly for style, lighting, and content
            val stream = ByteArrayOutputStream()
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L

            val frameCount = 5
            for (i in 0 until frameCount) {
                val timeUs = (duration * 1000 / frameCount) * i
                val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

                // Downscale to 480p and compress to 70% quality
                bitmap?.let {
                    val scaled = Bitmap.createScaledBitmap(it, 480, 854, false)
                    scaled.compress(Bitmap.CompressFormat.JPEG, 70, stream)
                }
            }
            stream.toByteArray()
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    fun release() {
        player.release()
    }
}