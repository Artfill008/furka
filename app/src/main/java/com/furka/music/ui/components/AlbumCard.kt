package com.furka.music.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

// Artifact B: The Expressive Album Card (Glass Edition)
// Uses Backdrop to blur the background *through* the card.

@Composable
fun ExpressiveAlbumCard(
    title: String,
    artist: String,
    backdrop: Backdrop, // Receives the liquid background
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f, // Bouncy scale
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // Glass Album Art Container
        // Instead of just a gradient, let's make it a "Lens" into the liquid background?
        // Or keep it as an album art holder. For "Furka", let's make the *container* glass
        // and put a placeholder icon/text inside.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(32.dp))
                // THE GLASS EFFECT:
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(32.dp) },
                    effects = {
                        vibrancy() // Boost colors
                        blur(32.dp.toPx()) // Heavy blur for frosted glass
                        // Lens effect for distortion at edges
                        lens(16.dp.toPx(), 32.dp.toPx()) 
                    },
                    onDrawSurface = {
                        // Tint it slightly
                        drawRect(Color.White.copy(alpha = 0.1f))
                    }
                )
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
