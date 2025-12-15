package com.furka.music.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.launch

/**
 * A reusable Glass Card for dialogs or floating content.
 */
@Composable
fun GlassCard(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(32.dp) },
                effects = {
                    with(density) {
                        vibrancy()
                        blur(radius = 20.dp.toPx()) // Heavy blur for "Frost" effect
                        lens(refractionHeight = 8.dp.toPx(), refractionAmount = 16.dp.toPx(), true)
                    }
                },
                onDrawSurface = {
                    // Frosty white tint
                    drawRect(Color.White.copy(alpha = 0.1f))
                }
            )
            .padding(24.dp)
    ) {
        content()
    }
}

/**
 * Interactive Glass Icon with "Press-to-Scale" physics.
 * Refracts the backdrop.
 */
@Composable
fun GlassIcon(
    backdrop: Backdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    val animationScope = rememberCoroutineScope()
    val progressAnimation = remember { Animatable(0f) }
    val density = LocalDensity.current

    Box(
        modifier
            .size(64.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    with(density) {
                        vibrancy()
                        blur(4.dp.toPx())
                        lens(16.dp.toPx(), 32.dp.toPx())
                    }
                },
                layerBlock = {
                    val progress = progressAnimation.value
                    // Scale content down on press, instead of up
                    val targetScale = 0.85f 
                    val scale = lerp(1f, targetScale, progress)
                    scaleX = scale
                    scaleY = scale
                },
                onDrawSurface = {
                    // Tinting logic
                    drawRect(tint.copy(alpha = 0.2f))
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .pointerInput(animationScope) {
                val animationSpec = spring<Float>(dampingRatio = 0.4f, stiffness = 400f) // LowBouncy
                awaitEachGesture {
                    awaitFirstDown()
                    animationScope.launch { progressAnimation.animateTo(1f, animationSpec) }
                    waitForUpOrCancellation()
                    animationScope.launch { progressAnimation.animateTo(0f, animationSpec) }
                }
            }
    )
}

/**
 * Liquid Glass Slider refactored from docs.
 */
@Composable
fun GlassSlider(
    backdrop: Backdrop,
    value: Float, // 0f to 1f
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .height(64.dp), // Touch target
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidth = constraints.maxWidth.toFloat()
        val thumbSize = 32.dp
        val thumbPx = with(density) { thumbSize.toPx() }
        
        // Calculate max scrollable width (width - thumb)
        val maxOffset = trackWidth - thumbPx
        
        fun updateValue(offsetX: Float) {
            val clamped = offsetX.coerceIn(0f, maxOffset)
            val percent = clamped / maxOffset
            onValueChange(percent)
        }
        
        val trackBackdrop = rememberLayerBackdrop()
        
        // Track Background (Glassy track)
        Box(
            Modifier
                .layerBackdrop(trackBackdrop)
                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                .height(4.dp)
                .fillMaxWidth()
        )

        // Calculate current visual offset
        val xOffset = with(density) { (maxOffset * value).toDp() }

        val combinedBackdrop = rememberCombinedBackdrop(backdrop, trackBackdrop)

        Box(
            Modifier
                .offset(x = xOffset)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        // Calculate new value based on delta
                        // We need to current offset in px to add delta?
                        // Actually easier: calculate new offset from current value + delta
                        val currentPx = maxOffset * value
                        val newPx = currentPx + delta
                        updateValue(newPx)
                    },
                    onDragStopped = { 
                        onValueChangeFinished?.invoke(value)
                    }
                )
                .drawBackdrop(
                    backdrop = combinedBackdrop, 
                    shape = { CircleShape },
                    effects = {
                         with(density) {
                            lens(
                                refractionHeight = 12.dp.toPx(),
                                refractionAmount = 16.dp.toPx(),
                                chromaticAberration = true
                            )
                        }
                    }
                )
                .size(56.dp, 32.dp)
        )
    }
}
