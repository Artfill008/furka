package com.furka.music.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.furka.music.data.model.AudioQualityInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object MetadataHelper {
    private val cache = mutableMapOf<Uri, AudioQualityInfo>()

    suspend fun getAudioQuality(context: Context, uri: Uri): AudioQualityInfo = withContext(Dispatchers.IO) {
        // Return cached value if available
        cache[uri]?.let { return@withContext it }

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            val mimetype = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "audio/unknown"
            val bitrateRaw = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull() ?: 0L
            val sampleRateRaw = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull() ?: 0
            
            // Format
            val format = when {
                mimetype.contains("flac") -> "FLAC"
                mimetype.contains("mpeg") || mimetype.contains("mp3") -> "MP3"
                mimetype.contains("mp4") || mimetype.contains("aac") -> "AAC"
                mimetype.contains("wav") -> "WAV"
                mimetype.contains("ogg") -> "OGG"
                else -> mimetype.substringAfterLast("/")
            }.uppercase()

            // Bitrate (kbps)
            val bitrate = if (bitrateRaw > 0) "${bitrateRaw / 1000} kbps" else "Unknown"

            // Sample Rate
            val sampleRate = if (sampleRateRaw > 0) {
                if (sampleRateRaw >= 1000) "${sampleRateRaw / 1000}kHz" else "${sampleRateRaw}Hz"
            } else ""

            // Bit Depth Estimation
            // BitDepth = (Bitrate / SampleRate / Channels)
            // This is an estimation as Android doesn't expose bit depth directly for all formats easily via MetadataRetriever
            // But for FLAC this calculation usually holds true for uncompressed stream equivalent
            // However, a safer fallback is mostly 16-bit for standard files, 24-bit for Hi-Res.
            var bitDepth = ""
            if (bitrateRaw > 0 && sampleRateRaw > 0) {
                 // Try to guess channel count (usually 2 for stereo music)
                 // If we could get channel count from metadata, that would be better.
                 // METADATA_KEY_NUM_TRACKS is often not audio channels. 
                 // Let's rely on standard high-res markers.
                 // A very rough heuristic:
                 // 1411kbps (CD) = 44.1 * 16 * 2
                 // If bitrate / sample rate > 32 (meaning 16 bit * 2 channels), it might be 24 bit.
                 val bitsPerSample = bitrateRaw / sampleRateRaw / 2 // assuming stereo
                 
                 bitDepth = if (bitsPerSample > 16) "24bit" else "16bit"
            }

            val info = AudioQualityInfo(sampleRate, bitDepth, bitrate, format)
            cache[uri] = info
            return@withContext info

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext AudioQualityInfo("", "", "Unknown", "FILE")
        } finally {
            retriever.release()
        }
    }
}
