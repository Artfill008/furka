package com.furka.music.data.model

import android.net.Uri

data class AudioTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val albumId: Long,
    val duration: Long,
    val contentUri: Uri,
    val albumArtUri: Uri,
    val size: Long
) {
    val bitrate: String
        get() {
            if (duration <= 0) return "Unknown"
            // kbps = (size_in_bytes * 8) / (duration_in_seconds * 1000)
            val kbps = (size * 8) / (duration / 1000) / 1000
            
            // Guess format (MediaStore usually has MIME type but we'll infer simply or use extension logic later if needed)
            // For now, just showing bitrate is a huge leap usually associated with quality.
            return "$kbps kbps"
        }
}
