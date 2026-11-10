package com.nunchuk.android.compose

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The grey filled button paired under [NcPrimaryDarkButton] (e.g. "Keep current setup", "Add key"). */
@Composable
fun NcSecondaryButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    height: Dp = 48.dp,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        enabled = enabled,
        modifier = modifier.height(height),
        onClick = onClick,
        content = {
            CompositionLocalProvider(
                LocalTextStyle provides NunchukTheme.typography.title.copy(color = Color.Unspecified),
            ) {
                content()
            }
        },
        shape = RoundedCornerShape(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.greyLight,
            contentColor = MaterialTheme.colorScheme.textPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.greyLight,
            disabledContentColor = MaterialTheme.colorScheme.textSecondary,
        ),
    )
}

@PreviewLightDark
@Composable
private fun NcSecondaryButtonPreview() {
    NunchukTheme {
        NcSecondaryButton(onClick = {}) {
            Text(text = "Secondary Button")
        }
    }
}
