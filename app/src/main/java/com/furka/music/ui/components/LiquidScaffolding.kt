package com.furka.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.vibrancy

// Artifact A (Foundation): The Liquid Scaffolding
// Updated to capture *content* into the backdrop so floating glass elements can see it.

@Composable
fun LiquidScaffold(
    modifier: Modifier = Modifier,
    content: @Composable (com.kyant.backdrop.backdrops.LayerBackdrop) -> Unit
) {
    // 1. Create the Layer Backdrop
    val backdrop = rememberLayerBackdrop {
        // Draw the Organic Background Mesh into the backdrop
        drawRect(
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF1A1A2E), // Deep Midnight
                    Color(0xFF16213E), // Dark Blue
                    Color(0xFF0F3460)  // Rich Navy
                )
            )
        )
        // CRITICAL: Capture content drawn with Modifier.layerBackdrop(backdrop)
        drawContent()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 2. Draw the visible background
        // We draw the backdrop *without* content (content hasn't been drawn yet) or effects
        // effectively drawing the gradient we just defined.
        // Note: Since 'backdrop' accumulates content, if we draw it here, we might get loops if we aren't careful.
        // BUT 'drawBackdrop' usually draws what's currently in the buffer.
        // To be safe and simple: let's just draw the SAME gradient here as the base, 
        // ensuring the user sees it, while the backdrop records it for the glass.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A2E),
                            Color(0xFF16213E),
                            Color(0xFF0F3460)
                        )
                    )
                )
        )

        // 3. Provide the backdrop to content
        content(backdrop)
    }
}
