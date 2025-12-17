package com.furka.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.furka.music.ui.theme.SyneFont
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.launch

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * GLASS SEARCH ISLAND
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * A floating glass capsule for search input.
 * - Floats at the top of the LibraryScreen.
 * - Visual symmetry with MiniPlayerIsland.
 * - Uses basic text field for transparency.
 */
@Composable
fun GlassSearchIsland(
    backdrop: Backdrop,
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceTintColor = MaterialTheme.colorScheme.surfaceTint
    val animationScope = rememberCoroutineScope()
    val progressAnimation = remember { Animatable(0f) }
    var isFocused by remember { mutableStateOf(false) }

    val animatedTint by animateColorAsState(
        targetValue = if (isFocused) {
            surfaceTintColor.copy(alpha = 0.4f) // Darker tint
        } else {
            surfaceTintColor.copy(alpha = 0.2f)
        },
        label = "tintAnimation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .pointerInput(animationScope) {
                val animationSpec = spring<Float>(dampingRatio = 0.4f, stiffness = 400f)
                awaitEachGesture {
                    awaitFirstDown()
                    animationScope.launch { progressAnimation.animateTo(1f, animationSpec) }
                    waitForUpOrCancellation()
                    animationScope.launch { progressAnimation.animateTo(0f, animationSpec) }
                }
            }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape }, // Capsule
                effects = {
                    vibrancy()
                    blur(10.dp.toPx())
                    lens(12.dp.toPx(), 24.dp.toPx())
                },
                layerBlock = {
                    val progress = progressAnimation.value
                    val targetScale = 0.95f
                    val scale = lerp(1f, targetScale, progress)
                    scaleX = scale
                    scaleY = scale
                },
                onDrawSurface = {
                    // Tinted glass using system tint color
                    drawRect(animatedTint)
                }
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                    },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search tracks...",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = SyneFont,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }
                }
            )
            
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
