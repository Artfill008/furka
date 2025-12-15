package com.furka.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens

// Artifact C: Glass Slider (Refined)
// Uses rememberCombinedBackdrop to refract both the Main Background (passed in)
// AND the Slider Track (captured locally).

@Composable
fun GlassSlider(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    progress: Float = 0.3f
) {
    BoxWithConstraints(
        modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .height(64.dp), // Touch target
        contentAlignment = Alignment.CenterStart
    ) {
        // 1. Capture the Track
        val trackBackdrop = rememberLayerBackdrop()
        
        // 2. The Track Content
        Box(
            Modifier
                .layerBackdrop(trackBackdrop)
                .background(Color.White.copy(alpha = 0.3f), CircleShape)
                .height(6.dp)
                .fillMaxWidth()
        ) {
            // Active part
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .height(6.dp)
                    .background(Color(0xFFCC2B5E), CircleShape)
            )
        }

        // 3. The Glass Thumb
        // Calculates position
        val thumbWidth = 56.dp
        val thumbHeight = 32.dp
        val availableWidth = maxWidth - thumbWidth
        val xOffset = availableWidth * progress
        
        // Combine the main world backdrop with our local track backdrop
        val combinedBackdrop = rememberCombinedBackdrop(backdrop, trackBackdrop)

        Box(
            Modifier
                .offset(x = xOffset)
                // Draw combined glass!
                .drawBackdrop(
                    backdrop = combinedBackdrop,
                    shape = { CircleShape },
                    effects = {
                        // Lens effect for distortion
                        lens(
                            refractionHeight = 12.dp.toPx(),
                            refractionAmount = 16.dp.toPx(),
                            chromaticAberration = true
                        )
                    }
                )
                .size(thumbWidth, thumbHeight)
        )
    }
}
