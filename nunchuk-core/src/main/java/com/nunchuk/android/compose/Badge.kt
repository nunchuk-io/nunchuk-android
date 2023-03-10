package com.nunchuk.android.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun NcBadge(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colors.onBackground,
    backgroundColor: Color = MaterialTheme.colors.background,
    content: @Composable RowScope.() -> Unit,
) {
    val radius = BadgeWithContentRadius
    val shape = RoundedCornerShape(radius)

    // Draw badge container.
    Row(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = shape
            )
            .border(width = 1.dp, color = borderColor, shape = shape)
            .clip(shape)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        content()
    }
}

private val BadgeWithContentRadius = 24.dp