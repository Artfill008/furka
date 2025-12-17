package com.furka.music.util

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.furka.music.data.model.AudioTrack

/**
 * Maps our domain [AudioTrack] to a Media3 [MediaItem] with full metadata.
 */
object MediaItemMapper {
    fun mapToMediaItem(track: AudioTrack): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setArtworkUri(track.albumArtUri) // Critical for notification image
            .setIsPlayable(true)
            .build()
            
        return MediaItem.Builder()
            .setUri(track.contentUri)
            .setMediaId(track.id.toString())
            .setMediaMetadata(metadata)
            .build()
    }

    fun mapToAudioTrack(mediaItem: MediaItem): AudioTrack {
        val metadata = mediaItem.mediaMetadata
        return AudioTrack(
            id = mediaItem.mediaId.toLongOrNull() ?: 0L,
            contentUri = mediaItem.requestMetadata.mediaUri ?: Uri.EMPTY,
            title = metadata.title?.toString() ?: "Unknown Title",
            artist = metadata.artist?.toString() ?: "Unknown Artist",
            albumId = 0L, // Placeholder, MediaItem doesn't carry our internal album ID
            duration = 0L, // Placeholder, duration is managed by the player state
            albumArtUri = metadata.artworkUri ?: Uri.EMPTY,
            size = 0L // Placeholder, not available from MediaItem
        )
    }
}
