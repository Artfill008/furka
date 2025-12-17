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

    // Media Controller Future
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController?
        get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null

    // UI State exposed to the screen
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
        })
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
        val player = controller ?: return
        val item = MediaItemMapper.mapToMediaItem(track)
        player.setMediaItem(item)
        player.prepare()
        player.play()
        
        // Update local state immediately for responsiveness
        _uiState.value = _uiState.value.copy(
            currentTrack = track,
            duration = track.duration.toFloat(),
            isPlaying = true
        )
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
        
        // Optimistic UI update
        if (tracks.isNotEmpty() && startIndex in tracks.indices) {
             _uiState.value = _uiState.value.copy(
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
        // We need to fetch the next item's ID and update currentTrack in UI
        // In a real app we'd observe MediaItemTransition
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
}


data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentPosition: Float = 0f,
    val duration: Float = 1f, // Avoid divide by zero
    val currentTrack: AudioTrack? = null,
    val currentIndex: Int = 0,
    val playlistSize: Int = 0
)
