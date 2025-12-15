package com.furka.music.data.manager

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.furka.music.data.model.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * AUDIO PLAYER MANAGER
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * ExoPlayer wrapper that manages:
 * - Single track playback
 * - Playlist with index navigation (Next/Prev)
 * - Shuffle mode
 * - Progress updates
 */
class AudioPlayerManager(context: Context) {

    private val player = ExoPlayer.Builder(context).build()
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    // Playlist state
    private var playlist: List<AudioTrack> = emptyList()
    private var currentIndex: Int = 0
    private var isShuffleEnabled: Boolean = false

    private val _uiState = MutableStateFlow(PlayerState())
    val uiState: StateFlow<PlayerState> = _uiState.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _uiState.value = _uiState.value.copy(duration = player.duration)
                }
                if (playbackState == Player.STATE_ENDED) {
                    // Auto-advance to next track
                    if (hasNext()) {
                        skipToNext()
                    } else {
                        stopProgressLoop()
                        _uiState.value = _uiState.value.copy(isPlaying = false, currentPosition = 0)
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
                if (isPlaying) {
                    startProgressLoop()
                } else {
                    stopProgressLoop()
                }
            }
        })
    }

    /**
     * Play a single track (used when clicking from library)
     */
    fun playTrack(track: AudioTrack) {
        // Single track mode
        playlist = listOf(track)
        currentIndex = 0
        playCurrentIndex()
    }

    /**
     * Set a playlist and play from a specific track
     */
    fun playPlaylist(tracks: List<AudioTrack>, startIndex: Int = 0) {
        playlist = tracks
        currentIndex = startIndex.coerceIn(0, tracks.size - 1)
        playCurrentIndex()
    }

    /**
     * Shuffle: Pick a random track from playlist and play it
     */
    fun shufflePlay(tracks: List<AudioTrack>) {
        if (tracks.isEmpty()) return
        playlist = tracks.shuffled()
        currentIndex = 0
        isShuffleEnabled = true
        playCurrentIndex()
    }

    /**
     * Skip to next track
     */
    fun skipToNext() {
        if (playlist.isEmpty()) return
        currentIndex = (currentIndex + 1) % playlist.size
        playCurrentIndex()
    }

    /**
     * Skip to previous track
     */
    fun skipToPrevious() {
        if (playlist.isEmpty()) return
        
        // If we're more than 3 seconds into the song, restart it
        // Otherwise go to previous
        if (player.currentPosition > 3000) {
            player.seekTo(0)
        } else {
            currentIndex = if (currentIndex > 0) currentIndex - 1 else playlist.size - 1
            playCurrentIndex()
        }
    }

    fun hasNext(): Boolean = playlist.isNotEmpty() && currentIndex < playlist.size - 1

    fun hasPrevious(): Boolean = playlist.isNotEmpty() && currentIndex > 0

    fun resume() {
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _uiState.value = _uiState.value.copy(currentPosition = positionMs)
    }

    fun release() {
        player.release()
        stopProgressLoop()
    }

    private fun playCurrentIndex() {
        if (playlist.isEmpty() || currentIndex !in playlist.indices) return
        
        val track = playlist[currentIndex]
        _uiState.value = _uiState.value.copy(
            currentTrack = track,
            currentIndex = currentIndex,
            playlistSize = playlist.size
        )
        
        val mediaItem = MediaItem.fromUri(track.contentUri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    private fun startProgressLoop() {
        stopProgressLoop()
        progressJob = scope.launch {
            while (isActive) {
                _uiState.value = _uiState.value.copy(currentPosition = player.currentPosition)
                delay(100) // Update every 100ms
            }
        }
    }

    private fun stopProgressLoop() {
        progressJob?.cancel()
        progressJob = null
    }
    
    companion object {
        @Volatile
        private var INSTANCE: AudioPlayerManager? = null

        fun getInstance(context: Context): AudioPlayerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AudioPlayerManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

data class PlayerState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0,
    val duration: Long = 0,
    val currentTrack: AudioTrack? = null,
    val currentIndex: Int = 0,
    val playlistSize: Int = 0
)
