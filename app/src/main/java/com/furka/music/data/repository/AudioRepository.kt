package com.furka.music.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.furka.music.data.SourceMode
import com.furka.music.data.SettingsDataStore
import com.furka.music.data.model.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class AudioRepository(
    private val context: Context,
    private val settingsDataStore: SettingsDataStore = SettingsDataStore(context)
) {

    suspend fun getAudioTracks(): List<AudioTrack> = withContext(Dispatchers.IO) {
        val settings = settingsDataStore.settingsFlow.first()
        return@withContext when (settings.sourceMode) {
            SourceMode.CASUAL -> loadFromMediaStore()
            SourceMode.AUDIOPHILE -> {
                val uriString = settings.selectedUri
                if (uriString.isNullOrEmpty()) {
                    // Fallback to MediaStore if something went wrong.
                    loadFromMediaStore()
                } else {
                    loadFromFolder(Uri.parse(uriString))
                }
            }
        }
    }

    private suspend fun loadFromMediaStore(): List<AudioTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<AudioTrack>()
        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.IS_MUSIC
        )

        // Filter for music only
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn)
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val albumId = cursor.getLong(albumIdColumn)
                val duration = cursor.getLong(durationColumn)
                val size = cursor.getLong(sizeColumn)

                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                
                val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
                val albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId)

                tracks.add(
                    AudioTrack(
                        id = id,
                        title = title,
                        artist = artist,
                        albumId = albumId,
                        duration = duration,
                        contentUri = contentUri,
                        albumArtUri = albumArtUri,
                        size = size
                    )
                )
            }
        }
        return@withContext tracks
    }

    /**
     * Walk a user-selected folder tree and collect FLAC/MP3/etc.
     * Runs strictly on Dispatchers.IO.
     */
    private suspend fun loadFromFolder(rootUri: Uri): List<AudioTrack> = withContext(Dispatchers.IO) {
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri)
        if (rootDoc == null || !rootDoc.isDirectory || !rootDoc.canRead()) {
            return@withContext emptyList()
        }

        val result = mutableListOf<AudioTrack>()
        walkDocumentTree(rootDoc, result)
        return@withContext result
    }

    private fun walkDocumentTree(node: DocumentFile, out: MutableList<AudioTrack>) {
        if (!node.isDirectory) return
        val children = node.listFiles()
        for (child in children) {
            if (child.isDirectory) {
                walkDocumentTree(child, out)
            } else if (child.isFile && child.canRead()) {
                val name = child.name ?: continue
                val lower = name.lowercase()
                // Only index typical music formats; FLAC-first.
                if (lower.endsWith(".flac") || lower.endsWith(".wav") || lower.endsWith(".mp3") || lower.endsWith(".m4a")) {
                    val uri = child.uri
                    // Try to resolve some basic metadata via MediaStore when possible.
                    val track = resolveAudioTrackFromUri(uri, name)
                    out.add(track)
                }
            }
        }
    }

    private fun resolveAudioTrackFromUri(uri: Uri, fallbackTitle: String): AudioTrack {
        // Attempt to map the DocumentFile URI to a MediaStore row for richer metadata.
        val resolver = context.contentResolver
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE
        )

        resolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)) ?: fallbackTitle
                val artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)) ?: "Unknown Artist"
                val albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
                val duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE))

                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
                val albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId)

                return AudioTrack(
                    id = id,
                    title = title,
                    artist = artist,
                    albumId = albumId,
                    duration = duration,
                    contentUri = contentUri,
                    albumArtUri = albumArtUri,
                    size = size
                )
            }
        }

        // Fallback when MediaStore metadata is unavailable.
        return AudioTrack(
            id = uri.hashCode().toLong(),
            title = fallbackTitle,
            artist = "Unknown Artist",
            albumId = -1L,
            duration = 0L,
            contentUri = uri,
            albumArtUri = Uri.EMPTY,
            size = 0L
        )
    }
}
