package com.furka.music.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.furka.music.data.SettingsDataStore
import com.furka.music.data.SourceMode
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.launch

/**
 * First‑run "Setup Portal" screen.
 *
 * Two giant glass cards:
 *  - High Fidelity (folder selection)
 *  - Scan Everything (MediaStore)
 */
@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsDataStore = remember { SettingsDataStore(context) }

    // Read theme colors in composable scope, then capture inside non-composable lambdas
    val backgroundColor = MaterialTheme.colorScheme.background

    val backdrop = rememberLayerBackdrop {
        // Soft atmospheric gradient background
        drawRect(backgroundColor)
        drawContent()
    }

    var pendingMode by remember { mutableStateOf<SourceMode?>(null) }

    // Folder picker launcher for Audiophile Mode
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        val mode = pendingMode
        if (uri != null && mode == SourceMode.AUDIOPHILE) {
            // Persist read permissions for the chosen tree
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )

            scope.launch {
                settingsDataStore.setSourceMode(SourceMode.AUDIOPHILE, uri.toString())
                settingsDataStore.markSetupComplete()
                onSetupComplete()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .safeContentPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Configure Furka",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Choose how you want Furka to discover your music.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Card A – Audiophile Mode (folder)
            GlassChoiceCard(
                backdrop = backdrop,
                icon = Icons.Rounded.FolderOpen,
                title = "High Fidelity",
                subtitle = "Select a specific folder. Keeps your library clean.",
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                pendingMode = SourceMode.AUDIOPHILE
                folderPickerLauncher.launch(null)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Card B – Casual Mode (scan everything)
            GlassChoiceCard(
                backdrop = backdrop,
                icon = Icons.Rounded.Layers,
                title = "Scan Everything",
                subtitle = "Finds all audio.",
                accentColor = MaterialTheme.colorScheme.secondary,
                showWarningCapsule = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                pendingMode = SourceMode.CASUAL
                scope.launch {
                    settingsDataStore.setSourceMode(SourceMode.CASUAL, selectedUri = null)
                    settingsDataStore.markSetupComplete()
                    onSetupComplete()
                }
            }
        }
    }
}

@Composable
private fun GlassChoiceCard(
    backdrop: com.kyant.backdrop.Backdrop,
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    showWarningCapsule: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val pressProgress = remember { Animatable(0f) }

    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = 350f
    )

    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(32.dp) },
                effects = {
                    vibrancy()
                    blur(18.dp.toPx())
                    lens(20.dp.toPx(), 40.dp.toPx())
                },
                layerBlock = {
                    val scale = 1f - (pressProgress.value * 0.04f)
                    scaleX = scale
                    scaleY = scale
                },
                onDrawSurface = {
                    // Slightly tinted to accent color but very glassy
                    drawRect(Color.Black.copy(alpha = 0.55f))
                }
            )
            .clip(RoundedCornerShape(32.dp))
            .clickable(onClick = {
                scope.launch {
                    pressProgress.animateTo(1f, springSpec)
                    pressProgress.animateTo(0f, springSpec)
                }
                onClick()
            })
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { CircleShape },
                            effects = {
                                vibrancy()
                                blur(12.dp.toPx())
                                lens(14.dp.toPx(), 24.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(accentColor.copy(alpha = 0.5f))
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.2.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (showWarningCapsule) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .clip(RoundedCornerShape(999.dp))
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedCornerShape(999.dp) },
                            effects = {
                                vibrancy()
                                blur(10.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color.Black.copy(alpha = 0.6f))
                            }
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = accentColor.copy(alpha = 0.9f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Warning: May include voice notes & system sounds.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}


