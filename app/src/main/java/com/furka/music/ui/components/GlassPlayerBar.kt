package com.furka.music.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop

/**
 * Glass Player Bar: The "Jewelry" control panel for the Player Screen.
 * 
 * Contains:
 * - Song metadata (title, artist) using Expressive typography
 * - LiquidControls (glass skip/play buttons with physics)
 * - GlassSlider (seek bar with combined backdrop refraction)
 */
@Composable
fun GlassPlayerBar(
    backdrop: Backdrop,
    trackTitle: String,
    artistName: String,
    currentPosition: Float, // ms
    duration: Float, // ms
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit, // absolute position ms
    onSeekFinished: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        // Song Info (Large, Expressive - Syne font)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = trackTitle,
                style = MaterialTheme.typography.displaySmall, // Huge
                maxLines = 1,
                color = Color.White
            )
            Text(
                text = artistName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Controls Row - All Glass with Physics
        LiquidControls(
            backdrop = backdrop,
            isPlaying = isPlaying,
            onPlayPause = onPlayPause,
            onSkipPrev = { /* TODO: Wire up skip prev */ },
            onSkipNext = { /* TODO: Wire up skip next */ },
            accentColor = Color(0xFFFF0055) // Heartbeat Pink
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Glass Slider (Seek)
        // Convert ms to 0-1
        val safeDuration = if (duration > 0) duration else 1f
        val progress = (currentPosition / safeDuration).coerceIn(0f, 1f)
        
        GlassSlider(
            backdrop = backdrop,
            value = progress,
            onValueChange = { percent ->
                onSeek(percent * safeDuration)
            },
            onValueChangeFinished = { percent ->
                onSeekFinished(percent * safeDuration)
            }
        )
    }
}
