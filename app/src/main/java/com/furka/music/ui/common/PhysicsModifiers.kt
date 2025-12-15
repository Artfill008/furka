package com.furka.music.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

// Factor A: "Heavy" Spring Physics
// Damping 0.7 = Low Bouncy (Organic)
// Stiffness 300 = Medium High (Responsive but substantial)
private const val SPRING_DAMPING = 0.7f
private const val SPRING_STIFFNESS = 300f

fun Modifier.pressClickEffect(
    enabled: Boolean = true,
    scaleDown: Float = 0.95f,
    onClick: (() -> Unit)? = null
) = composed {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = SPRING_DAMPING,
            stiffness = SPRING_STIFFNESS
        ),
        label = "PressScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enabled) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // ACTION_DOWN
                    isPressed = true
                    
                    val up = waitForUpOrCancellation()
                    // ACTION_UP or CANCEL
                    isPressed = false
                    
                    if (up != null && onClick != null) {
                         // Only trigger click if it was an UP event (not cancel)
                         // But we should let the standard clickable handle the actual click 
                         // if we want standard semantics. 
                         // However, to ensure synchronization, we can call it here.
                         // But standard clickable is better for accessibility.
                    }
                }
            }
        }
        // We use a separate clickable for semantics and accessibility if onClick is provided,
        // but suppress its visual ripple (indication = null).
        .then(
             if (onClick != null) {
                 Modifier.clickable(
                     interactionSource = remember { MutableInteractionSource() },
                     indication = null,
                     onClick = onClick
                 )
             } else Modifier
        )
}
