package com.furka.music.service

import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * FURKA PLAYBACK SERVICE
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * The heart of background playback.
 * - Hosts the ExoPlayer instance
 * - Manages the MediaSession
 * - Handles Notification System Integration
 */
class FurkaPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        // Default behavior: when IDLE/STOPPED, service stops background execution.
        // We can add custom behaviors here if needed.
        
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        // Return session to any controller allowed strictly inside our app or system
        // For simplicity we allow all connections (system notification is external)
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0 || player.playbackState == Player.STATE_ENDED) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
