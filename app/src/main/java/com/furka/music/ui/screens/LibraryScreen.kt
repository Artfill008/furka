package com.furka.music.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.furka.music.data.model.AudioTrack
import com.furka.music.ui.components.CleanPermissionUI
import com.furka.music.ui.components.GlassSearchIsland
import com.furka.music.ui.components.MiniPlayerIsland
import com.furka.music.ui.viewmodel.LibraryUiState
import com.furka.music.ui.viewmodel.LibraryViewModel
import com.furka.music.ui.viewmodel.PlayerViewModel
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * FURKA LIBRARY SCREEN
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * Design: CLEAN & STRUCTURAL with one piece of "Jewelry"
 * 
 * Structure (NO GLASS):
 *   - Solid deep matte background
 *   - Expressive list items with physics scale
 *   - Search bar at top
 * 
 * Jewelry (GLASS):
 *   - Floating Shuffle FAB with lens effect
 */



@Composable
fun LibraryScreen(
    onNavigateToPlayer: () -> Unit,
    viewModel: LibraryViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    forceScan: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    val playerState by playerViewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Fast Scroller State
    var currentDragChar by remember { mutableStateOf<Char?>(null) }
    var bubbleYPosition by remember { mutableStateOf(0f) }

    LaunchedEffect(forceScan) {
        viewModel.loadLibrary()
    }

    // Backdrop for the Glass FAB
    val backgroundColor = MaterialTheme.colorScheme.background
    val backdrop = rememberLayerBackdrop {
        drawRect(backgroundColor)
        drawContent()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0) // EDGE-TO-EDGE FIX: Disable default insets
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // ═══════════════════════════════════════════════════════════════════════
            // LAYER A: SOURCE CONTENT (Captured)
            // ═══════════════════════════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .layerBackdrop(backdrop)
            ) {
                when (val state = uiState) {
                    is LibraryUiState.Loading -> {
                        LoadingState()
                    }
                    is LibraryUiState.PermissionDenied -> {
                        CleanPermissionUI(
                            onPermissionGranted = { viewModel.onPermissionResult(true) }
                        )
                    }
                    is LibraryUiState.Empty -> {
                        EmptyState()
                    }
                    is LibraryUiState.Success -> {
                        // Local, composable‑side filtering to avoid recomposing the whole
                        // list tree more than needed. Uses the cached allTracks list.
                        val filteredTracks by remember(state.allTracks, searchQuery) {
                            derivedStateOf {
                                if (searchQuery.isBlank()) {
                                    state.allTracks
                                } else {
                                    state.allTracks.filter { track ->
                                        track.title.contains(searchQuery, ignoreCase = true) ||
                                            track.artist.contains(searchQuery, ignoreCase = true)
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.fillMaxSize()) {
                            // List Area
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                TrackList(
                                    tracks = filteredTracks,
                                    listState = listState,
                                    onTrackClick = { index ->
                                        playerViewModel.playPlaylist(filteredTracks, index)
                                        onNavigateToPlayer()
                                    }
                                )

                                // Fast Scroller
                                if (state.sections.size > 1 && searchQuery.isEmpty()) {
                                    FastScroller(
                                        sections = state.sections,
                                        onSectionSelected = { char, yPos ->
                                            currentDragChar = char
                                            bubbleYPosition = yPos
                                            val index = state.fastScrollIndex[char]
                                            if (index != null) {
                                                scope.launch { listState.scrollToItem(index) }
                                            }
                                        },
                                        onDragEnd = {
                                            currentDragChar = null
                                        },
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .padding(end = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // ═══════════════════════════════════════════════════════════════════════
            // LAYER B: JEWELRY (Consumer - Sibling to Source)
            // ═══════════════════════════════════════════════════════════════════════
            if (uiState is LibraryUiState.Success) {
                val state = uiState as LibraryUiState.Success
                
                // Shuffle FAB (use the full, unfiltered list for shuffle)
                GlassShuffleFab(
                    backdrop = backdrop,
                    onClick = {
                        if (state.allTracks.isNotEmpty()) {
                            playerViewModel.shufflePlay(state.allTracks)
                            onNavigateToPlayer()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 160.dp, end = 16.dp)
                )
                
                // Glass Search Island (Top)
                GlassSearchIsland(
                    backdrop = backdrop,
                    query = searchQuery,
                    onQueryChange = { query ->
                        viewModel.onSearchQueryChange(query)
                    },
                    onClear = {
                        viewModel.onSearchQueryChange("")
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = 16.dp)
                )
                
                // Mini Player Island
                if (playerState.currentTrack != null) {
                    MiniPlayerIsland(
                        backdrop = backdrop,
                        currentTrack = playerState.currentTrack,
                        isPlaying = playerState.isPlaying,
                        onTogglePlayPause = { playerViewModel.togglePlayPause() },
                        onSkipNext = { playerViewModel.skipToNext() },
                        onSkipPrevious = { playerViewModel.skipToPrevious() },
                        onExpand = onNavigateToPlayer,
                        onClick = onNavigateToPlayer,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(bottom = 8.dp) // Float above nav bar
                    )
                }
                
                // Fast Scroll Bubble
                if (currentDragChar != null) {
                    FastScrollBubble(
                         backdrop = backdrop,
                         char = currentDragChar!!,
                         yOffset = bubbleYPosition,
                         modifier = Modifier
                             .align(Alignment.TopEnd)
                             // Offset to match the list area visually
                             .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 80.dp)
                             .padding(end = 48.dp) 
                    )
                }
            }
        }
    }
}    



// ═══════════════════════════════════════════════════════════════════════════════
// GLASS SHUFFLE FAB (Jewelry - Floating Water Drop)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun GlassShuffleFab(
    backdrop: Backdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val haptic = LocalHapticFeedback.current
    val primaryColor = MaterialTheme.colorScheme.primary
    
    // Physics Spring for press animation
    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = 350f
    )

    Box(
        modifier = modifier
            .size(64.dp)
            .scale(scale.value)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    vibrancy()
                    blur(16.dp.toPx())
                    lens(
                        refractionHeight = 20.dp.toPx(),
                        refractionAmount = 40.dp.toPx()
                    )
                },
                onDrawSurface = {
                    // Tinted glass using system primary color
                    drawRect(primaryColor.copy(alpha = 0.3f))
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                scope.launch {
                    // Heavy haptic feedback
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    
                    // Physics press animation
                    scale.animateTo(0.85f, springSpec)
                    scale.animateTo(1f, springSpec)
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Shuffle,
            contentDescription = "Shuffle Play",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// LOADING STATE
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.secondary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Scanning Library...",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// EMPTY STATE
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Folder,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "No Music Found",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add some FLAC files to your device",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TRACK LIST
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun TrackList(
    tracks: List<AudioTrack>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onTrackClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 110.dp, // Space for Glass Search Island + Status Bar
            bottom = 144.dp, // Space for mini player + FAB + Nav Bar
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            LibraryHeader(trackCount = tracks.size)
        }
        
        // Track Items with animation
        itemsIndexed(
            items = tracks,
            key = { _, track -> track.id }
        ) { index, track ->
            LibraryItemSwipe(
                track = track,
                onPrimaryAction = { onTrackClick(index) },
                onQueueAction = {
                    // TODO: wire to "Add to Queue" in ViewModel when available.
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// LIBRARY HEADER
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun LibraryHeader(trackCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Library",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$trackCount ${if (trackCount == 1) "track" else "tracks"}",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TRACK LIST ITEM
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun TrackListItemContent(
    track: AudioTrack,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArtThumbnail(
            albumArtUri = track.albumArtUri,
            modifier = Modifier.size(56.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Async Real Metadata
        val context = LocalContext.current
        val qualityInfo by androidx.compose.runtime.produceState(initialValue = com.furka.music.data.model.AudioQualityInfo("", "", "...", "FLAC")) {
            value = com.furka.music.util.MetadataHelper.getAudioQuality(context, track.contentUri)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${qualityInfo.format} • ${qualityInfo.bitrate}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = com.furka.music.ui.theme.ManropeFont,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
            )
            if (qualityInfo.sampleRate.isNotEmpty()) {
                Text(
                    text = "${qualityInfo.sampleRate} ${if(qualityInfo.bitDepth.isNotEmpty()) "/ ${qualityInfo.bitDepth}" else ""}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = com.furka.music.ui.theme.ManropeFont,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

/**
 * Library list swipe container:
 * - **Primary tap:** plays the track immediately.
 * - **Swipe gesture:** reveals an "Add to Queue" glass/gradient background.
 * - **Haptics:** strong vibration when the swipe action commits.
 */
@Composable
private fun LibraryItemSwipe(
    track: AudioTrack,
    onPrimaryAction: () -> Unit,
    onQueueAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }

    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val dismissState: SwipeToDismissBoxState = rememberSwipeToDismissBoxState(
        initialValue = SwipeToDismissBoxValue.Settled,
        confirmValueChange = { value: SwipeToDismissBoxValue ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                // Swiped left → Add to Queue
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onQueueAction()
            }
            // Always return false so the item visually snaps back to "Settled".
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            // Glassy / gradient background, not destructive red.
            val brush = Brush.horizontalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(brush)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Add to Queue",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = "Add to Queue",
                        tint = Color.White
                    )
                }
            }
        },
        content = {
            Row(
                modifier = modifier
                    .scale(scale.value)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        scope.launch {
                            scale.animateTo(0.97f, springSpec)
                            scale.animateTo(1f, springSpec)
                        }
                        onPrimaryAction()
                    }
            ) {
                TrackListItemContent(
                    track = track
                )
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// ALBUM ART THUMBNAIL
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun AlbumArtThumbnail(
    albumArtUri: android.net.Uri,
    modifier: Modifier = Modifier
) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(albumArtUri)
            .crossfade(true)
            .build(),
        contentDescription = "Album Art",
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// UTILITIES
// ═══════════════════════════════════════════════════════════════════════════════


// ═══════════════════════════════════════════════════════════════════════════════
// FAST SCROLLER (Structure - Interactive)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun FastScroller(
    sections: List<Char>,
    onSectionSelected: (Char, Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var columnHeightPx by remember { mutableStateOf(0f) }
    var lastSelectedChar by remember { mutableStateOf<Char?>(null) }

    fun getCharAndPos(yPos: Float): Pair<Char, Float>? {
        if (columnHeightPx == 0f || sections.isEmpty()) return null
        
        val itemHeight = columnHeightPx / sections.size
        val index = (yPos / itemHeight).toInt().coerceIn(0, sections.size - 1)
        val char = sections[index]
        
        // Calculate center Y of this char for bubble positioning
        val centerY = (index * itemHeight) + (itemHeight / 2)
        return char to centerY
    }

    Column(
        modifier = modifier
            .width(24.dp)
            .padding(vertical = 16.dp)
            .pointerInput(sections) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        val result = getCharAndPos(offset.y)
                        if (result != null) {
                            val (char, yPos) = result
                            if (char != lastSelectedChar) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                lastSelectedChar = char
                            }
                            onSectionSelected(char, offset.y)
                        }
                    },
                    onVerticalDrag = { change, _ ->
                        val result = getCharAndPos(change.position.y)
                        if (result != null) {
                            val (char, yPos) = result
                            if (char != lastSelectedChar) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                lastSelectedChar = char
                                onSectionSelected(char, change.position.y)
                            }
                        }
                    },
                    onDragEnd = {
                        lastSelectedChar = null
                        onDragEnd()
                    }
                )
            }
            .onGloballyPositioned { coordinates ->
                columnHeightPx = coordinates.size.height.toFloat()
            },
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        sections.forEach { char ->
            Text(
                text = char.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp // Tiny tech font
                ),
                color = if (char == lastSelectedChar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// FAST SCROLL BUBBLE (Jewelry - Glass Overlay)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun FastScrollBubble(
    backdrop: Backdrop,
    char: Char,
    yOffset: Float,
    modifier: Modifier = Modifier
) {
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    // Bubble floats next to the finger
    Box(
        modifier = modifier
            .offset(y = with(LocalDensity.current) { yOffset.toDp() - 32.dp }) // Center the 64dp bubble
            .size(64.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    vibrancy()
                    blur(8.dp.toPx())
                    // Magnifying lens effect
                    lens(
                        refractionHeight = 24.dp.toPx(),
                        refractionAmount = 30.dp.toPx()
                    )
                },
                onDrawSurface = {
                    drawRect(onSurfaceColor.copy(alpha = 0.1f))
                    drawRect(primaryColor.copy(alpha = 0.2f))
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = char.toString(),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold
            ),
            color = Color.White
        )
    }
}
