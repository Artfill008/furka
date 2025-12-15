package com.furka.music.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.launch

/**
 * "Water Droplet" control cluster for Player Screen
 * 
 * All controls use Backdrop glass with physics animations:
 * - Skip buttons: Small glass circles with lens effect
 * - Play button: Large glass circle with tinted accent
 * 
 * Physics: Spring.DampingRatioLowBouncy (0.7) for "heavy" organic feel
 */
@Composable
fun LiquidControls(
    backdrop: Backdrop,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSkipPrev: () -> Unit,
    onSkipNext: () -> Unit,
    accentColor: Color = Color(0xFFFF0055), // Heartbeat Pink
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Skip Previous (Small Glass)
        GlassControlButton(
            backdrop = backdrop,
            icon = Icons.Rounded.SkipPrevious,
            contentDescription = "Previous",
            onClick = onSkipPrev,
            size = 56.dp,
            iconSize = 28.dp
        )
        
        // Play/Pause (Large Tinted Glass - Centerpiece)
        TintedGlassPlayButton(
            backdrop = backdrop,
            isPlaying = isPlaying,
            onClick = onPlayPause,
            tint = accentColor,
            size = 88.dp,
            iconSize = 40.dp
        )
        
        // Skip Next (Small Glass)
        GlassControlButton(
            backdrop = backdrop,
            icon = Icons.Rounded.SkipNext,
            contentDescription = "Next",
            onClick = onSkipNext,
            size = 56.dp,
            iconSize = 28.dp
        )
    }
}

/**
 * Small glass control button with physics press animation.
 * Used for skip prev/next buttons.
 */
@Composable
fun GlassControlButton(
    backdrop: Backdrop,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp = 56.dp,
    iconSize: Dp = 28.dp,
    modifier: Modifier = Modifier
) {
    val animationScope = rememberCoroutineScope()
    val progressAnimation = remember { Animatable(0f) }
    
    // Heavy spring: Low damping + low stiffness = organic feel
    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = 400f
    )

    Box(
        modifier = modifier
            .size(size)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    vibrancy()
                    blur(8.dp.toPx())
                    lens(12.dp.toPx(), 24.dp.toPx())
                },
                layerBlock = {
                    // Physics scale: content shrinks, backdrop stays stable
                    val progress = progressAnimation.value
                    val scale = lerp(1f, 0.85f, progress)
                    scaleX = scale
                    scaleY = scale
                },
                onDrawSurface = {
                    // Subtle white frost
                    drawRect(Color.White.copy(alpha = 0.15f))
                }
            )
            .pointerInput(animationScope) {
                awaitEachGesture {
                    awaitFirstDown()
                    animationScope.launch { progressAnimation.animateTo(1f, springSpec) }
                    
                    waitForUpOrCancellation()
                    onClick()
                    animationScope.launch { progressAnimation.animateTo(0f, springSpec) }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Large tinted glass play/pause button.
 * Uses BlendMode.Hue to tint the refracted backdrop.
 */
@Composable
fun TintedGlassPlayButton(
    backdrop: Backdrop,
    isPlaying: Boolean,
    onClick: () -> Unit,
    tint: Color = Color(0xFFFF0055),
    size: Dp = 88.dp,
    iconSize: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val animationScope = rememberCoroutineScope()
    val progressAnimation = remember { Animatable(0f) }
    
    // "Heavy" spring for premium feel
    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = 300f
    )

    Box(
        modifier = modifier
            .size(size)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    vibrancy()
                    blur(12.dp.toPx())
                    lens(24.dp.toPx(), 48.dp.toPx())
                },
                layerBlock = {
                    // Physics: scale down on press, backdrop stable
                    val progress = progressAnimation.value
                    val scale = lerp(1f, 0.9f, progress)
                    scaleX = scale
                    scaleY = scale
                },
                onDrawSurface = {
                    // Tinted glass using BlendMode.Hue (from docs)
                    drawRect(tint, blendMode = BlendMode.Hue)
                    drawRect(tint.copy(alpha = 0.3f))
                }
            )
            .pointerInput(animationScope) {
                awaitEachGesture {
                    awaitFirstDown()
                    animationScope.launch { progressAnimation.animateTo(1f, springSpec) }
                    
                    waitForUpOrCancellation()
                    onClick()
                    animationScope.launch { progressAnimation.animateTo(0f, springSpec) }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}
