package com.anai.app

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects // THE FIX: It moved to the transformer package!
import androidx.media3.common.audio.AudioProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(UnstableApi::class)
class MediaManager(private val context: Context) {

    val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        repeatMode = Player.REPEAT_MODE_ONE
    }

    fun loadVideo(uri: Uri) {
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    suspend fun squeezeVideoForAi(inputUri: Uri, onProgress: (String) -> Unit): File? = withContext(Dispatchers.IO) {
        val outputFile = File(context.cacheDir, "ai_upload_temp.mp4")
        if (outputFile.exists()) outputFile.delete()

        onProgress("Optimizing video to 720p...")

        val videoEffects = listOf<Effect>(Presentation.createForHeight(720))

        // Using the correct transformer.Effects class
        val effects = Effects(listOf<AudioProcessor>(), videoEffects)

        val editedMediaItem = EditedMediaItem.Builder(MediaItem.fromUri(inputUri))
            .setEffects(effects)
            .build()

        onProgress("Squeezer structure ready.")
        outputFile
    }

    fun release() {
        player.release()
    }
}