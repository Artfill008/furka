package com.furka.music.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.furka.music.R
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.remember

// Artifact A: Glass Navigation Bar
// Floating, high-fidelity glass bar.
// Uses vibrancy, blur, and lens for "expensive" look.

@Composable
fun GlassNavigationBar(
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val navInsets = WindowInsets.navigationBars.asPaddingValues()
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp) // Floating margin
            .padding(navInsets) // Respect safe area
    ) {
        Row(
            modifier = Modifier
                .height(80.dp)
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(24.dp) },
                    effects = {
                        vibrancy()
                        blur(32.dp.toPx()) // Deep frosted blur
                        // Lens effect for edge distortion/refraction
                        // Signature: lens(refractionHeight, refractionAmount)
                        lens(12.dp.toPx(), 24.dp.toPx())
                    },
                    onDrawSurface = {
                        // Expressive Twist: Frosted Tint
                        // Instead of plain white, a very subtle cool tint
                        drawRect(Color(0xFFE0E0FF).copy(alpha = 0.05f))
                    }
                ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(icon = Icons.Rounded.Home, label = "Home")
            NavItem(icon = Icons.Rounded.Search, label = "Search")
            NavItem(icon = Icons.Rounded.LibraryMusic, label = "Library")
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String) {
    Box(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // No ripple, maybe custom later
            ) {}
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier
        )
    }
}
