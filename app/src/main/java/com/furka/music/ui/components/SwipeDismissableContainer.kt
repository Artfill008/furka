package com.furka.music.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A container that can be swiped down to dismiss.
 * - Scales down as you drag (depth effect)
 * - Fades out
 * - Snaps back or Dismisses based on threshold
 */
@Composable
fun SwipeDismissableContainer(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    
    // Physics Spring
    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    val draggableState = rememberDraggableState { delta ->
        scope.launch {
            // Only allow dragging down (positive delta) unless already dragged
            if (offsetY.value + delta >= 0f) {
                // Resistance logic
                val current = offsetY.value
                val resistance = 1f + (current / 1000f)
                offsetY.snapTo(current + delta / resistance)
            }
        }
    }
    
    // Calculate visuals based on drag progress.
    // Use ~30% of screen height as our "intuitive" dismiss gesture band.
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val visualRange = (screenHeightPx * 0.6f).coerceAtLeast(with(density) { 400.dp.toPx() })
    val dragProgress = (offsetY.value / visualRange).coerceIn(0f, 1f)
    val scale = 1f - (dragProgress * 0.15f) // Shrink to ~85% at full drag
    val alpha = 1f - dragProgress

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .scale(scale)
            .alpha(alpha)
            .draggable(
                state = draggableState,
                orientation = Orientation.Vertical,
                onDragStopped = {
                    val threshold = (screenHeightPx * 0.3f).coerceAtLeast(with(density) { 150.dp.toPx() })
                    if (offsetY.value > threshold) {
                        // Dismiss
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            offsetY.animateTo(with(density) { 1000.dp.toPx() }, springSpec)
                            onDismiss()
                        }
                    } else {
                        // Snap back
                        scope.launch {
                            offsetY.animateTo(0f, springSpec)
                        }
                    }
                }
            )
    ) {
        content()
    }
}
