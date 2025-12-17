package com.furka.music.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.background
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.draw.clip
import com.kyant.backdrop.backdrops.layerBackdrop

@Composable
fun GlassPlayerControl(
    backdrop: Backdrop,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSkipPrev: () -> Unit,
    onSkipNext: () -> Unit,
    progress: Float,
    onSliderChange: (Float) -> Unit,
    onSliderChangeFinished: (Float) -> Unit,
    accentColor: Color,
    currentPosition: Long,
    duration: Long,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProductionGlassSlider(
            backdrop = backdrop,
            value = progress,
            onValueChange = onSliderChange,
            onValueChangeFinished = onSliderChangeFinished,
            accentColor = accentColor,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPosition),
                color = Color.White.copy(alpha = 0.5f)
            )
            Text(
                text = formatTime(duration),
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PhysicsGlassButton(
                backdrop = backdrop,
                icon = Icons.Rounded.SkipPrevious,
                contentDescription = "Previous",
                onClick = onSkipPrev,
                size = 64.dp,
                iconSize = 32.dp
            )

            PhysicsGlassButton(
                backdrop = backdrop,
                icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                onClick = onPlayPause,
                size = 88.dp,
                iconSize = 44.dp,
                tint = accentColor,
                isCenterpiece = true
            )

            PhysicsGlassButton(
                backdrop = backdrop,
                icon = Icons.Rounded.SkipNext,
                contentDescription = "Next",
                onClick = onSkipNext,
                size = 64.dp,
                iconSize = 32.dp
            )
        }
    }
}

@Composable
internal fun PhysicsGlassButton(
    backdrop: Backdrop,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp,
    iconSize: Dp,
    tint: Color? = null,
    isCenterpiece: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val pressProgress = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current

    val springSpec = spring<Float>(
        dampingRatio = 0.7f,
        stiffness = 350f
    )

    Box(
        modifier = modifier
            .size(size)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    vibrancy()
                    blur(if (isCenterpiece) 12.dp.toPx() else 8.dp.toPx())
                    lens(
                        refractionHeight = if (isCenterpiece) 20.dp.toPx() else 12.dp.toPx(),
                        refractionAmount = if (isCenterpiece) 40.dp.toPx() else 24.dp.toPx()
                    )
                },
                layerBlock = {
                    val scale = lerp(1f, 0.88f, pressProgress.value)
                    scaleX = scale
                    scaleY = scale
                },
                onDrawSurface = {
                    if (tint != null && isCenterpiece) {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = 0.5f))
                    } else {
                        drawRect(Color.White.copy(alpha = 0.2f))
                    }
                }
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch { pressProgress.animateTo(1f, springSpec) }

                    waitForUpOrCancellation()
                    onClick()
                    scope.launch { pressProgress.animateTo(0f, springSpec) }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
internal fun ProductionGlassSlider(
    backdrop: Backdrop,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (Float) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    BoxWithConstraints(
        modifier = modifier.height(48.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val trackBackdrop = rememberLayerBackdrop()
        val combinedBackdrop = rememberCombinedBackdrop(backdrop, trackBackdrop)

        val trackWidth = constraints.maxWidth.toFloat()
        val thumbWidth = with(density) { 48.dp.toPx() }
        val maxOffset = trackWidth - thumbWidth

        Box(
            modifier = Modifier
                .layerBackdrop(trackBackdrop)
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(value)
                    .height(4.dp)
                    .background(accentColor.copy(alpha = 0.6f), CircleShape)
            )
        }

        val xOffset = with(density) { (maxOffset * value).toDp() }

        Box(
            modifier = Modifier
                .padding(start = xOffset)
                .size(48.dp, 28.dp)
                .drawBackdrop(
                    backdrop = combinedBackdrop,
                    shape = { CircleShape },
                    effects = {
                        lens(
                            refractionHeight = 10.dp.toPx(),
                            refractionAmount = 14.dp.toPx(),
                            chromaticAberration = true
                        )
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.08f))
                    }
                )
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        var currentX = down.position.x + (maxOffset * value)
                        var lastHapticValue = value

                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull()
                            if (change != null && change.pressed) {
                                val delta = change.positionChange()
                                currentX += delta.x
                                val newValue = (currentX / maxOffset).coerceIn(0f, 1f)

                                if (kotlin.math.abs(newValue - lastHapticValue) > 0.05f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    lastHapticValue = newValue
                                }

                                onValueChange(newValue)
                                change.consume()
                            }
                        } while (event.changes.any { it.pressed })

                        val finalValue = (currentX / maxOffset).coerceIn(0f, 1f)
                        onValueChangeFinished(finalValue)
                    }
                }
        )
    }
}

private fun formatTime(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format("%d:%02d", minutes, seconds)
}
