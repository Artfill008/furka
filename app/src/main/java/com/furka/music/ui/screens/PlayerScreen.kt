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
                Text(
                    text = uiState.currentTrack?.title ?: "No Track",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Artist Name - Manrope (via theme)
                Text(
                    text = uiState.currentTrack?.artist ?: "Select a song",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // ═══════════════════════════════════════════════════════════════════
            // SLIDER (Jewelry - Glass Refraction)
            // Bound directly to ViewModel's fraction‑based slider APIs
            // ═══════════════════════════════════════════════════════════════════
            val safeDuration = if (uiState.duration > 0) uiState.duration else 1f
            val progress = (uiState.currentPosition / safeDuration).coerceIn(0f, 1f)

            ProductionGlassSlider(
                backdrop = backdrop,
                value = progress,
                onValueChange = { fraction ->
                    // fraction in 0f..1f, VM converts to ms internally
                    viewModel.onSliderChange(fraction)
                },
                onValueChangeFinished = { fraction ->
                    viewModel.onSliderChangeFinished(fraction)
                },
                accentColor = animatedVibrant,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Time Labels - Manrope (Technical font)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(uiState.currentPosition.toLong()),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Text(
                    text = formatTime(uiState.duration.toLong()),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
            
            Spacer(modifier = Modifier.weight(0.3f))
            
            // ═══════════════════════════════════════════════════════════════════
            // CONTROLS (Jewelry - Glass Bubbles)
            // ═══════════════════════════════════════════════════════════════════
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Skip Previous
                PhysicsGlassButton(
                    backdrop = backdrop,
                    icon = Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    onClick = { viewModel.skipToPrevious() },
                    size = 64.dp,
                    iconSize = 32.dp
                )
                
                // Play/Pause - Centerpiece (Tinted Glass)
                PhysicsGlassButton(
                    backdrop = backdrop,
                    icon = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                    onClick = { viewModel.togglePlayPause() },
                    size = 88.dp,
                    iconSize = 44.dp,
                    tint = animatedVibrant,
                    isCenterpiece = true
                )
                
                // Skip Next
                PhysicsGlassButton(
                    backdrop = backdrop,
                    icon = Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    onClick = { viewModel.skipToNext() },
                    size = 64.dp,
                    iconSize = 32.dp
                )
            }
            
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
                    drawRect(Color.White.copy(alpha = 0.05f))
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

// ═══════════════════════════════════════════════════════════════════════════════
// PHYSICS GLASS BUTTON (Jewelry - Lens Effect + Spring Scale)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun PhysicsGlassButton(
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
    
    // Physics Spring: Heavy, Rubbery feel
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
                    // Physics scale in layerBlock: backdrop stays stable
                    val scale = lerp(1f, 0.88f, pressProgress.value)
                    scaleX = scale
                    scaleY = scale
                },
                onDrawSurface = {
                    if (tint != null && isCenterpiece) {
                        // Tinted glass using BlendMode.Hue
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = 0.35f))
                    } else {
                        drawRect(Color.White.copy(alpha = 0.12f))
                    }
                }
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    // Haptic feedback for a more tangible feel
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

// ═══════════════════════════════════════════════════════════════════════════════
// PRODUCTION GLASS SLIDER (Jewelry - Combined Backdrop Refraction)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun ProductionGlassSlider(
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
        
        // Track Background
        Box(
            modifier = Modifier
                .layerBackdrop(trackBackdrop)
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
        ) {
            // Active track fill
            Box(
                modifier = Modifier
                    .fillMaxWidth(value)
                    .height(4.dp)
                    .background(accentColor.copy(alpha = 0.6f), CircleShape)
            )
        }
        
        // Glass Thumb
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
                        
                        // Haptic on grab
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull()
                            if (change != null && change.pressed) {
                                val delta = change.positionChange()
                                currentX += delta.x
                                val newValue = (currentX / maxOffset).coerceIn(0f, 1f)
                                
                                // Haptic tick every 5% of progress
                                if (kotlin.math.abs(newValue - lastHapticValue) > 0.05f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    lastHapticValue = newValue
                                }
                                
                                onValueChange(newValue)
                                change.consume()
                            }
                        } while (event.changes.any { it.pressed })
                        
                        // Finished
                        val finalValue = (currentX / maxOffset).coerceIn(0f, 1f)
                        onValueChangeFinished(finalValue)
                    }
                }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// UTILITIES
// ═══════════════════════════════════════════════════════════════════════════════
private fun formatTime(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format("%d:%02d", minutes, seconds)
}
