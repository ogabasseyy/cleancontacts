package com.ogabassey.contactscleaner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ogabassey.contactscleaner.ui.theme.GlassBorder
import com.ogabassey.contactscleaner.ui.theme.GlassWhite

fun Modifier.glassy(
    radius: Dp = 24.dp,
    borderWidth: Dp = 1.dp,
    color: Color = Color.Transparent,
    strokeColor: Color = Color.Transparent
): Modifier {
    val showCustomBorder = strokeColor != Color.Transparent
    
    return this
    .shadow(
        elevation = 12.dp,
        shape = RoundedCornerShape(radius),
        clip = false,
        ambientColor = Color.Black.copy(alpha = 0.8f),
        spotColor = Color.Black.copy(alpha = 0.8f)
    )
    .clip(RoundedCornerShape(radius))
    .background(color) // Apply base tint
    .background(
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.04f)
            )
        )
    )
    .border(
        width = borderWidth,
        brush = if (showCustomBorder) {
            androidx.compose.ui.graphics.SolidColor(strokeColor)
        } else {
             Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.3f),
                    Color.White.copy(alpha = 0.05f)
                )
             )
        },
        shape = RoundedCornerShape(radius)
    )
}
