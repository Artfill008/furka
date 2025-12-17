package com.furka.music.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.furka.music.ui.components.FrostedGlassText
import com.furka.music.ui.components.GlassPlayerControl
import com.furka.music.ui.components.SwipeDismissableContainer
import com.furka.music.ui.viewmodel.PlayerViewModel
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * FURKA PLAYER SCREEN
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * Visual Layer Architecture:
 * 
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ Layer 0: ATMOSPHERE                                                         │
 * │   - Radial gradient from album art palette (dominant → muted)              │
 * │   - Massively blurred album art overlay (blur = 64dp)                      │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *                                    ↓
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ Layer 1: BACKDROP SOURCE (rememberLayerBackdrop)                           │
 * │   - Captures everything for glass refraction                               │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *                                    ↓
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ Layer 2: STRUCTURE (Material 3 - No Glass)                                 │
 * │   - Album Art Card (with subtle glass depth)                               │
 * │   - Track Title (Syne ExtraBold)                                           │
 * │   - Artist Name (Manrope Medium)                                           │
 * │   - Time Labels (Manrope)                                                  │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *                                    ↓
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ Layer 3: JEWELRY (Backdrop Glass)                                          │
 * │   - Glass Slider (rememberCombinedBackdrop for track + background)         │
 * │   - Play Button (Tinted Lens Effect)                                       │
 * │   - Skip Buttons (Subtle Lens Effect)                                      │
 * └─────────────────────────────────────────────────────────────────────────────┘
 * 
 * Physics: All interactions use Spring(damping=0.7, stiffness=350)
 * Typography: Syne (Display) + Manrope (Technical)
 */
@Composable
fun PlayerScreen(
    onDismiss: () -> Unit,
    viewModel: PlayerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PALETTE EXTRACTION STATE
    // ═══════════════════════════════════════════════════════════════════════════
    var dominantColor by remember { mutableStateOf(Color(0xFF6B4EE6)) } // Default: Deep Purple
    var vibrantColor by remember { mutableStateOf(Color(0xFFFF0055)) }  // Accent: Heartbeat Pink
    var mutedColor by remember { mutableStateOf(Color(0xFF1A1A2E)) }    // Background: Midnight
    var darkMutedColor by remember { mutableStateOf(Color(0xFF0F0F1A)) } // Deepest shadow
    
    // Physics-based color animation (Anti-Slop: NO tween!)
    // Using Spring for organic color transitions
    val animatedDominant by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "dominant"
    )
    
    val animatedVibrant by animateColorAsState(
        targetValue = vibrantColor,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "vibrant"
    )
    
    val animatedMuted by animateColorAsState(
        targetValue = mutedColor,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "muted"
    )
    
    val animatedDarkMuted by animateColorAsState(
        targetValue = darkMutedColor,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "darkMuted"
    )

    // ═══════════════════════════════════════════════════════════════════════════
    // LAYER 1: BACKDROP SOURCE
    // ═══════════════════════════════════════════════════════════════════════════
    val backdrop = rememberLayerBackdrop {
        // Draw atmospheric gradient into backdrop
        drawRect(
            Brush.verticalGradient(
                colors = listOf(
                    animatedMuted,
                    animatedDarkMuted
                )
            )
        )
        drawContent()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedDarkMuted)
    ) {
        SwipeDismissableContainer(onDismiss = onDismiss) {
            // ═══════════════════════════════════════════════════════════════════════
            // LAYER 0: ATMOSPHERE (Gradient + Blurred Art)
            // ═══════════════════════════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxSize()
                .layerBackdrop(backdrop)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            animatedDominant.copy(alpha = 0.6f),
                            animatedMuted.copy(alpha = 0.8f),
                            animatedDarkMuted
                        ),
                        radius = 1400f
                    )
                )
        ) {
            // Massively blurred album art (atmospheric backdrop)
            if (uiState.currentTrack != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(uiState.currentTrack!!.albumArtUri)
                        .allowHardware(false)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(radius = 64.dp)
                        .alpha(0.4f),
                    onSuccess = { result ->
                        val bitmap = (result.result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                        if (bitmap != null) {
                            Palette.from(bitmap).generate { palette ->
                                // Extract full color palette for rich atmosphere
                                palette?.dominantSwatch?.rgb?.let { dominantColor = Color(it) }
                                palette?.vibrantSwatch?.rgb?.let { vibrantColor = Color(it) }
                                    ?: run { vibrantColor = Color(0xFFFF0055) }
                                palette?.mutedSwatch?.rgb?.let { mutedColor = Color(it) }
                                palette?.darkMutedSwatch?.rgb?.let { darkMutedColor = Color(it) }
                                    ?: run { darkMutedColor = mutedColor.copy(alpha = 0.5f) }
                            }
                        }
                    }
                )
            }
        }

        // ═══════════════════════════════════════════════════════════════════════
        // LAYER 2 & 3: STRUCTURE + JEWELRY
        // ═══════════════════════════════════════════════════════════════════════
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.5f))
            
            // ═══════════════════════════════════════════════════════════════════
            // ALBUM ART CARD (Structure with subtle glass depth)
            // ═══════════════════════════════════════════════════════════════════
            GlassAlbumArt(
                backdrop = backdrop,
                albumArtUri = uiState.currentTrack?.albumArtUri,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // ═══════════════════════════════════════════════════════════════════
            // METADATA (Structure - No Glass, Beautiful Typography)
            // ═══════════════════════════════════════════════════════════════════
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Track Title - Syne ExtraBold (via theme)
                FrostedGlassText(
                    text = uiState.currentTrack?.title ?: "No Track",
                    backdrop = backdrop,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Artist Name - Manrope (via theme)
                FrostedGlassText(
                    text = uiState.currentTrack?.artist ?: "Select a song",
                    backdrop = backdrop,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // ═══════════════════════════════════════════════════════════════════
            // SLIDER (Jewelry - Glass Refraction)
            // Bound directly to ViewModel's fraction‑based slider APIs
            // ═══════════════════════════════════════════════════════════════════
            val safeDuration = if (uiState.duration > 0) uiState.duration else 1f
            val progress = (uiState.currentPosition / safeDuration).coerceIn(0f, 1f)

            GlassPlayerControl(
                backdrop = backdrop,
                isPlaying = uiState.isPlaying,
                onPlayPause = { viewModel.togglePlayPause() },
                onSkipPrev = { viewModel.skipToPrevious() },
                onSkipNext = { viewModel.skipToNext() },
                progress = progress,
                onSliderChange = { fraction ->
                    viewModel.onSliderChange(fraction)
                },
                onSliderChangeFinished = { fraction ->
                    viewModel.onSliderChangeFinished(fraction)
                },
                accentColor = animatedVibrant,
                currentPosition = uiState.currentPosition.toLong(),
                duration = uiState.duration.toLong()
            )

            Spacer(modifier = Modifier.weight(0.3f))
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// GLASS ALBUM ART (Jewelry - Subtle Depth)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun GlassAlbumArt(
    backdrop: Backdrop,
    albumArtUri: android.net.Uri?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(32.dp) },
                effects = {
                    vibrancy()
                    blur(8.dp.toPx())
                    lens(8.dp.toPx(), 16.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.15f))
                }
            )
            .clip(RoundedCornerShape(32.dp))
    ) {
        if (albumArtUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(albumArtUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Placeholder gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF2D1B69),
                                Color(0xFF11001C)
                            )
                        )
                    )
            )
        }
    }
}
