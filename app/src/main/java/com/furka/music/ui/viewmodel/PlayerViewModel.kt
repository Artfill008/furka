package com.furka.music.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.furka.music.data.model.AudioTrack
import com.furka.music.service.FurkaPlaybackService
import com.furka.music.util.MediaItemMapper
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    // Media Controller Future
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController?
        get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null

    // UI State exposed to the screen
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    // Slider state decoupling
    private var isDraggingSlider = false
    private var dragPosition: Float = 0f

    init {
        initializeController()

        // Polling loop for progress updates. This is a pragmatic choice for smooth UI
        // when the Player.Listener events for position are not frequent enough.
        viewModelScope.launch {
            while (true) {
                if (controller?.isPlaying == true && !isDraggingSlider) {
                    _uiState.value = _uiState.value.copy(
                        currentPosition = controller?.currentPosition?.toFloat() ?: _uiState.value.currentPosition
                    )
                }
                delay(200) // 5Hz is enough for smooth progress
            }
        }
    }

    private fun initializeController() {
        val sessionToken = SessionToken(
            getApplication(),
            android.content.ComponentName(getApplication(), FurkaPlaybackService::class.java)
        )

        controllerFuture = MediaController.Builder(getApplication(), sessionToken).buildAsync()
        controllerFuture?.addListener({
            // Controller connected
            setupPlayerListener()
            // Initial Sync
            updateUiState()
        }, MoreExecutors.directExecutor())
    }

    private fun setupPlayerListener() {
        controller?.addListener(object : Player.Listener {
            // onEvents is a catch-all that is perfect for ensuring UI sync
            override fun onEvents(player: Player, events: Player.Events) {
                updateUiState()
            }
        })
    }

    private fun updateUiState() {
        val player = controller ?: return
        val currentMediaItem = player.currentMediaItem

        // Find the current track from our stored playlist. This is crucial for keeping
        // the UI (title, artist, art) in sync when tracks change automatically.
        val currentTrack = if (currentMediaItem != null) {
            _uiState.value.playlist.find { it.id.toString() == currentMediaItem.mediaId }
        } else {
            _uiState.value.currentTrack
        }

        _uiState.value = _uiState.value.copy(
            isPlaying = player.isPlaying,
            duration = player.duration.toFloat().coerceAtLeast(1f),
            playlistSize = player.mediaItemCount,
            currentTrack = currentTrack,
            // Only update position from the listener if not dragging, to prevent jumps
            currentPosition = if (!isDraggingSlider) player.currentPosition.toFloat() else _uiState.value.currentPosition
        )
    }


    override fun onCleared() {
        super.onCleared()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PLAYBACK CONTROLS
    // ═══════════════════════════════════════════════════════════════════════════

    fun playTrack(track: AudioTrack) {
        playPlaylist(listOf(track), 0)
    }

    fun playPlaylist(tracks: List<AudioTrack>, startIndex: Int = 0) {
        val player = controller ?: return

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val items = tracks.map { MediaItemMapper.mapToMediaItem(it) }

            withContext(kotlinx.coroutines.Dispatchers.Main) {
                player.setMediaItems(items, startIndex, androidx.media3.common.C.TIME_UNSET)
                player.prepare()
                player.repeatMode = Player.REPEAT_MODE_ALL
                player.play()
            }
        }

        // Optimistic UI update and store the playlist for future reference
        if (tracks.isNotEmpty() && startIndex in tracks.indices) {
            _uiState.value = _uiState.value.copy(
                playlist = tracks, // <-- CRITICAL: Store the playlist
                currentTrack = tracks[startIndex],
                duration = tracks[startIndex].duration.toFloat(),
                isPlaying = true
            )
        }
    }

    fun shufflePlay(tracks: List<AudioTrack>) {
        val player = controller ?: return
        if (tracks.isEmpty()) return

        val shuffled = tracks.shuffled()
        playPlaylist(shuffled, 0)
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun skipToNext() {
        controller?.seekToNextMediaItem()
        // UI will update automatically via the Player.Listener calling updateUiState()
    }

    fun skipToPrevious() {
        controller?.seekToPreviousMediaItem()
        // UI will update automatically via the Player.Listener calling updateUiState()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SLIDER CONTROLS (Decoupled State)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Called continuously while user is dragging the slider.
     */
    fun onSliderChange(value: Float) {
        isDraggingSlider = true
        val newPosition = value * (_uiState.value.duration)
        _uiState.value = _uiState.value.copy(currentPosition = newPosition)
    }

    /**
     * Called when user releases the slider.
     */
    fun onSliderChangeFinished(value: Float) {
        isDraggingSlider = false
        val newPosition = value * (_uiState.value.duration)
        controller?.seekTo(newPosition.toLong())
        // Immediately sync with player position to prevent slider jumping back
        _uiState.value = _uiState.value.copy(currentPosition = newPosition)
    }
}


data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentPosition: Float = 0f,
    val duration: Float = 1f, // Avoid divide by zero
    val currentTrack: AudioTrack? = null,
    val currentIndex: Int = 0,
    val playlistSize: Int = 0,
    val playlist: List<AudioTrack> = emptyList() // Added to resolve track transitions
)
