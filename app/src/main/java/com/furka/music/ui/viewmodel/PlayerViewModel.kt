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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

data class PlayerUiState(
    val currentTrack: TrackDetails? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Float = 0f,
    val duration: Float = 1f
)

data class TrackDetails(
    val title: String,
    val artist: String,
    val albumArtUri: Uri?
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var mediaController: MediaController? = null
    private val executor = Executors.newSingleThreadExecutor()

    init {
        val sessionToken = SessionToken(application, ComponentName(application, FurkaPlaybackService::class.java))
        val controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                mediaController = controllerFuture.get()
                mediaController?.addListener(playerListener)
                updateUiState()
                startProgressUpdater()
            },
            executor
        )
    }

    private fun startProgressUpdater() = viewModelScope.launch {
        while (isActive) {
            if (mediaController?.isPlaying == true) {
                _uiState.value = _uiState.value.copy(
                    currentPosition = mediaController?.currentPosition?.toFloat() ?: 0f
                )
            }
            delay(500)
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
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
        mediaController?.seekToPreviousMediaItem()
    }

    fun onSliderChange(fraction: Float) {
        _uiState.value = _uiState.value.copy(currentPosition = fraction * _uiState.value.duration)
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
