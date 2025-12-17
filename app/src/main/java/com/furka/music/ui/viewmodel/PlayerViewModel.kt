package com.furka.music.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.furka.music.data.model.Track
import com.furka.music.service.FurkaPlaybackService
import com.furka.music.util.MediaItemMapper
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * PLAYER VIEW MODEL
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * Manages playback UI state with proper slider state decoupling:
 * 
 * SLIDER LOGIC (Critical Fix):
 * - `sliderPosition`: The value displayed to the user
 * - `isDragging`: Boolean flag
 * 
 * When isDragging = TRUE  → Display drag value, don't update from player
 * When isDragging = FALSE → Display player current position
 * 
 * Seek is ONLY called on `onValueChangeFinished`
 */
class PlayerViewModel : AndroidViewModel {

data class TrackDetails(
    val title: String,
    val artist: String,
    val albumArtUri: Uri?
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    // Slider state decoupling
    private var isDraggingSlider = false

    // Progress polling job
    private var progressJob: Job? = null

    // Main constructor for the app
    constructor(application: Application) : super(application) {
        initializeController()
    }

    // Constructor for testing
    internal constructor(
        application: Application,
        mediaController: MediaController
    ) : super(application) {
        this.controllerFuture = com.google.common.util.concurrent.Futures.immediateFuture(mediaController)
        this.controllerFuture?.addListener({
            setupPlayerListener()
            updateUiState()
        }, com.google.common.util.concurrent.MoreExecutors.directExecutor())
    }

    private fun initializeController() {
        val sessionToken = SessionToken(
            getApplication(),
            android.content.ComponentName(getApplication(), FurkaPlaybackService::class.java)
        )
    }

    private fun setupPlayerListener() {
        controller?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateUiState()
                if (isPlaying) {
                    startProgressUpdates()
                } else {
                    stopProgressUpdates()
                }
            }

            override fun onEvents(player: Player, events: Player.Events) {
                // We listen for specific events now, but a general update is fine
                // for things like media item transitions, timeline changes, etc.
                updateUiState()
            }
            delay(500)
        }
    }

    private fun updateUiState() {
        val player = controller ?: return

        // This is the primary state update from the player's own state.
        _uiState.value = _uiState.value.copy(
            isPlaying = player.isPlaying,
            duration = player.duration.toFloat().coerceAtLeast(1f),
            playlistSize = player.mediaItemCount,
            currentTrack = player.currentMediaItem?.let { MediaItemMapper.mapToAudioTrack(it) }
                ?: _uiState.value.currentTrack
        )

        // If not dragging, ensure slider position reflects player's actual position.
        if (!isDraggingSlider) {
            _uiState.value = _uiState.value.copy(
                currentPosition = player.currentPosition.toFloat()
            )
        }
    }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateUiState()
        }
    }

    private fun updateUiState() {
        val currentMediaItem = mediaController?.currentMediaItem ?: return
        val trackDetails = TrackDetails(
            title = currentMediaItem.mediaMetadata.title?.toString() ?: "Unknown Title",
            artist = currentMediaItem.mediaMetadata.artist?.toString() ?: "Unknown Artist",
            albumArtUri = currentMediaItem.mediaMetadata.artworkUri
        )
        _uiState.value = _uiState.value.copy(
            currentTrack = trackDetails,
            duration = mediaController?.duration?.toFloat() ?: 1f,
            currentPosition = mediaController?.currentPosition?.toFloat() ?: 0f
        )
    }

    fun playPlaylist(tracks: List<Track>, startIndex: Int) {
        val mediaItems = tracks.map { track ->
            MediaItem.Builder()
                .setUri(track.uri)
                .setMediaId(track.id.toString())
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setArtworkUri(track.albumArtUri)
                        .build()
                )
                .build()
        }
        mediaController?.setMediaItems(mediaItems, startIndex, 0)
        mediaController?.prepare()
        mediaController?.play()
    }

    fun shufflePlay(tracks: List<Track>) {
        val mediaItems = tracks.map { track ->
            MediaItem.Builder()
                .setUri(track.uri)
                .setMediaId(track.id.toString())
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setArtworkUri(track.albumArtUri)
                        .build()
                )
                .build()
        }
        mediaController?.setMediaItems(mediaItems)
        mediaController?.shuffleModeEnabled = true
        mediaController?.prepare()
        mediaController?.play()
    }

    fun togglePlayPause() {
        if (mediaController?.isPlaying == true) {
            mediaController?.pause()
        } else {
            mediaController?.play()
        }
    }

    fun skipToNext() {
        mediaController?.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        controller?.seekToPreviousMediaItem()
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SLIDER CONTROLS (Decoupled State)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Called continuously while user is dragging the slider.
     */
    fun onSliderChange(fraction: Float) {
        isDraggingSlider = true
        val newPosition = (_uiState.value.duration * fraction)
        // Update UI immediately for responsive feedback
        _uiState.value = _uiState.value.copy(currentPosition = newPosition)
    }

    /**
     * Called when user releases the slider.
     */
    fun onSliderChangeFinished(fraction: Float) {
        val seekPosition = (_uiState.value.duration * fraction).toLong()
        controller?.seekTo(seekPosition)
        isDraggingSlider = false
    }

    private fun startProgressUpdates() {
        // Cancel any existing job to prevent multiple coroutines running.
        stopProgressUpdates()
        progressJob = viewModelScope.launch {
            while (isActive) {
                if (!isDraggingSlider) {
                    _uiState.value = _uiState.value.copy(
                        currentPosition = controller?.currentPosition?.toFloat()
                            ?: _uiState.value.currentPosition
                    )
                }
                delay(200) // 5Hz is plenty for a smooth progress bar.
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    fun onSliderChangeFinished(fraction:Float) {
        mediaController?.seekTo((fraction * _uiState.value.duration).toLong())
    }

    override fun onCleared() {
        super.onCleared()
        mediaController?.release()
        executor.shutdown()
    }
}
