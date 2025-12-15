package com.furka.music.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.furka.music.data.model.AudioTrack
import com.furka.music.ui.modifiers.elasticDrag
import com.furka.music.ui.modifiers.elasticGesture
import com.furka.music.ui.theme.SyneFont
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.launch

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * LIQUID DYNAMIC ISLAND
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * A floating glass capsule that acts as the mini player.
 * Features:
 * - Liquid Glass visuals (vibrancy + blur + lens)
 * - Press-to-Scale physics
 * - Refracts the content behind it
 */
@Composable
fun MiniPlayerIsland(
    backdrop: Backdrop,
    currentTrack: AudioTrack?,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onExpand: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentTrack == null) return

    val animationScope = rememberCoroutineScope()
    val progressAnimation = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp) // Provide generous touch area
            .padding(horizontal = 16.dp, vertical = 8.dp)
            // Horizontal: track skipping (left → next, right → previous)
            .elasticGesture(
                orientation = androidx.compose.foundation.gestures.Orientation.Horizontal,
                onDragSuccess = { direction ->
                    if (direction > 0) {
                        onSkipPrevious()
                    } else {
                        onSkipNext()
                    }
                }
            )
            // Vertical: pan the island up to expand into full player
            .elasticGesture(
                orientation = androidx.compose.foundation.gestures.Orientation.Vertical,
                onDragSuccess = { direction ->
                    // Up = negative direction
                    if (direction < 0) {
                        onExpand()
                    }
                }
            )
    ) {
        // The Glass Capsule
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(50) }, // Continuous Capsule
                    effects = {
                        vibrancy()
                        blur(12.dp.toPx())
                        lens(16.dp.toPx(), 32.dp.toPx())
                    },
                    layerBlock = {
                        val progress = progressAnimation.value
                        val maxScale = 0.95f // Shrink slightly on press
                        val scale = androidx.compose.ui.util.lerp(1f, maxScale, progress)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = {
                        drawRect(Color.Black.copy(alpha = 0.6f))
                    }
                )
                .clip(RoundedCornerShape(50)) // Clip content to capsule
                .clickable(interactionSource = null, indication = null, onClick = onClick)
                .pointerInput(animationScope) {
                    val animationSpec = spring<Float>(stiffness = 300f)
                    awaitEachGesture {
                        awaitFirstDown()
                        animationScope.launch { progressAnimation.animateTo(1f, animationSpec) }
                        waitForUpOrCancellation()
                        animationScope.launch { progressAnimation.animateTo(0f, animationSpec) }
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art (Tiny Circle)
                AsyncImage(
                    model = currentTrack.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Track Info
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = currentTrack.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = SyneFont,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentTrack.artist,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Solid Play Button (Contrast against glass)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onTogglePlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            // Linear Progress Indicator (Bottom Edge)
            // Ideally we'd map this to real progress, but for "Island" mode,
            // a subtle hint is enough or we wire it up.
            // Let's keep it clean for now, or add a thin line at the bottom.
        }
    }
}
