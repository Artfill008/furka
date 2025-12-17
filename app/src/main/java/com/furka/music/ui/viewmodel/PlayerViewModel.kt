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

    // In-memory playlist
    private var currentPlaylist: List<AudioTrack> = emptyList()

    init {
        initializeController()
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
                }
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                updateUiState()
            }
        })
    }

    private fun startProgressUpdates() {
        viewModelScope.launch {
            while (controller?.isPlaying == true) {
                if (!isDraggingSlider) {
                    _uiState.value = _uiState.value.copy(
                        currentPosition = controller?.currentPosition?.toFloat() ?: 0f,
                        duration = controller?.duration?.toFloat()?.coerceAtLeast(1f) ?: 1f
                    )
                }
                delay(100)
            }
        }
    }

    private fun updateUiState() {
        val player = controller ?: return
        
        // Extract current track info from MediaMetadata if available, or maintain what we have.
        // Ideally we map MediaItem back to AudioTrack, or we store the current domain object separately.
        // For now, let's keep it simple: We need to parse the MediaItem tag or metadata.
        // Since we don't have perfect mapping back from MediaItem -> AudioTrack easily without passing IDs, 
        // we might rely on the fact that we passed the track in.
        // BUT: For notification sync, reliability comes from the Service state.
        
        // Let's rely on the previous logic of setting current track when playing,
        // and updating status from controller.
        
        // Note: Real implementation would query Repository by ID from mediaItem.mediaId
        val currentMediaId = player.currentMediaItem?.mediaId?.toLongOrNull()
        val currentTrack = currentPlaylist.find { it.id == currentMediaId }
        
        _uiState.value = _uiState.value.copy(
            isPlaying = player.isPlaying,
            duration = player.duration.toFloat().coerceAtLeast(1f),
            playlistSize = player.mediaItemCount,
            currentTrack = currentTrack ?: _uiState.value.currentTrack
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
        val player = controller ?: return
        currentPlaylist = listOf(track)
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
        currentPlaylist = tracks
        
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
    fun onSliderChange(value: Float) {
        isDraggingSlider = true
        dragPosition = value
        // Update UI immediately for responsive feedback
        _uiState.value = _uiState.value.copy(currentPosition = dragPosition)
    }

    /**
     * Called when user releases the slider.
     */
    fun onSliderChangeFinished(value: Float) {
        isDraggingSlider = false
        dragPosition = 0f
        controller?.seekTo(value.toLong())
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
