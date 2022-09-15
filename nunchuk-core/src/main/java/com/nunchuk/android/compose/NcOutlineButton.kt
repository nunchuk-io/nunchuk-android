package com.nunchuk.android.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun NcOutlineButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit, content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        modifier = modifier.height(48.dp),
        enabled = enabled,
        onClick = onClick,
        border = BorderStroke(2.dp, MaterialTheme.colors.primary),
        shape = RoundedCornerShape(44.dp),
        content = content,
        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent)
    )
}