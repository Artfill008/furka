package com.furka.music.ui.modifiers

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Adds an "Elastic" physical feel to a composable.
 *
 * Pan–dimensional rules:
 * - **Rubber banding:** movement sticks to the finger but fights back with increasing resistance.
 * - **Snap:** if the drag crosses the threshold, fire a **distinct haptic** and invoke [onDragSuccess].
 * - **Spring back:** regardless of success, the element eases home with a low‑bouncy spring.
 * - **Texture:** while dragging past ~40% of the threshold, emit subtle "tick" haptics (if available).
 */
fun Modifier.elasticDrag(
    orientation: Orientation,
    onDragSuccess: (direction: Int) -> Unit, // -1 for Start/Top, 1 for End/Bottom
    thresholdDp: Float = 96f // gesture-sized, tuned by density below
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    // Convert to pixels once for this composition
    val thresholdPx = with(density) { thresholdDp.dp.toPx() }
    
    // Physics Spring
    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    var lastTickEmittedForDirection by remember { mutableStateOf(0) }

    val draggableState = rememberDraggableState { delta ->
        scope.launch {
            // Apply resistance: The further we are, the harder it is to pull.
            // Formula: delta / (1 + |currentOffset| / factor)
            val current = offset.value
            val resistance = 1f + abs(current) / (thresholdPx * 0.6f).coerceAtLeast(1f)
            val dampedDelta = delta / resistance
            val newOffset = current + dampedDelta

            offset.snapTo(newOffset)

            // Continuous texture haptics once we've entered the "high resistance" zone (~40%).
            val progress = abs(newOffset) / thresholdPx
            val directionSign = sign(newOffset).toInt()
            if (progress > 0.4f && directionSign != 0 && lastTickEmittedForDirection != directionSign) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                lastTickEmittedForDirection = directionSign
            }
            if (progress < 0.2f) {
                // Reset so we can re‑emit ticks when the user reverses or retries.
                lastTickEmittedForDirection = 0
            }
        }
    }

    this
        .offset {
            if (orientation == Orientation.Horizontal)
                IntOffset(offset.value.roundToInt(), 0)
            else
                IntOffset(0, offset.value.roundToInt())
        }
        .draggable(
            state = draggableState,
            orientation = orientation,
            onDragStopped = {
                val dragAmount = offset.value
                val magnitude = abs(dragAmount)

                if (magnitude > thresholdPx) {
                    // Success: heavy, confirming haptic.
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val direction = sign(dragAmount).toInt().coerceIn(-1, 1)
                    if (direction != 0) onDragSuccess(direction)
                }

                // Always spring back to center.
                scope.launch {
                    offset.animateTo(0f, springSpec)
                }
            }
        )
}

/**
 * Alias with a more explicit name to match the design docs.
 *
 * This simply delegates to [elasticDrag] but gives you a semantic "gesture" label.
 */
fun Modifier.elasticGesture(
    orientation: Orientation,
    onDragSuccess: (direction: Int) -> Unit,
    thresholdDp: Float = 96f
): Modifier = elasticDrag(
    orientation = orientation,
    onDragSuccess = onDragSuccess,
    thresholdDp = thresholdDp
)
