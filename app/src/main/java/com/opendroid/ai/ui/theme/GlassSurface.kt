package com.opendroid.ai.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * ZonIA Glassmorphism surfaces.
 */

/** Glass card with blur background — premium iOS-style */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 16,
    borderAlpha: Float = 0.15f,
    content: @Composable () -> Unit
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(cornerRadius.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        colors.glassGradientStart,
                        colors.glassGradientEnd
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f)
                ),
                shape = shape
            )
            .border(
                width = 0.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = borderAlpha),
                        Color.Transparent,
                        Color.White.copy(alpha = borderAlpha * 0.5f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f)
                ),
                shape = shape
            )
    ) {
        content()
    }
}

/** Glass navigation bar background */
@Composable
fun GlassNavBar(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = AppTheme.colors
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.glassSurface,
                        colors.surface.copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                color = colors.glassBorder,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
    ) {
        content()
    }
}
