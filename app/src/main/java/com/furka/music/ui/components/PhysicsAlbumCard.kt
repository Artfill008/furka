package com.furka.music.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.launch

// Artifact B: Physics Album Card (Corrected)
// Uses layerBlock to scale content without scaling the backdrop (the glass).
// This prevents the "misplaced backdrop" issue described in the docs.

@Composable
fun PhysicsAlbumCard(
    title: String,
    artist: String,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val animationScope = rememberCoroutineScope()
    val progressAnimation = remember { Animatable(0f) }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(32.dp))
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(32.dp) },
                    effects = {
                        vibrancy()
                        blur(24.dp.toPx())
                        lens(16.dp.toPx(), 32.dp.toPx())
                    },
                    layerBlock = {
                        // CRITICAL: Logic from "Interactive Glass Bottom Bar" tutorial
                        // Apply scale transformation HERE.
                        val progress = progressAnimation.value
                        val scale = lerp(1f, 0.92f, progress) // Scale down to 0.92
                        
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.15f))
                    }
                )
                .pointerInput(animationScope) {
                    val animationSpec = spring<Float>(
                        dampingRatio = 0.7f,
                        stiffness = 300f
                    )
                    awaitEachGesture {
                        awaitFirstDown()
                        animationScope.launch {
                            progressAnimation.animateTo(1f, animationSpec)
                        }
                        
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            onClick()
                        }
                        animationScope.launch {
                            progressAnimation.animateTo(0f, animationSpec)
                        }
                    }
                }
        ) {
            Text(
                "FLAC",
                modifier = Modifier.align(Alignment.Center),
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.displayLarge
            )
        }

        // Metadata
        Column(modifier = Modifier.padding(top = 16.dp, start = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
