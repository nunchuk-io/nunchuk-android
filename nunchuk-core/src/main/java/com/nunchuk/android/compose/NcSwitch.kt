package com.nunchuk.android.compose

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark

@Composable
fun NcSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(
        uncheckedThumbColor = Color.White,
        checkedThumbColor = Color.White,
        uncheckedTrackColor = MaterialTheme.colorScheme.controlDefault,
        checkedTrackColor = MaterialTheme.colorScheme.controlActivated,
        uncheckedBorderColor = MaterialTheme.colorScheme.border,
        checkedBorderColor = MaterialTheme.colorScheme.controlActivated,
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        thumbContent = thumbContent,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource
    )
}

@Composable
@PreviewLightDark
fun NcSwitchPreview() {
    NunchukTheme {
        NcSwitch(
            checked = true,
            onCheckedChange = {},
        )
    }
}

@Composable
@PreviewLightDark
fun NcSwitchDefaultPreview() {
    NunchukTheme {
        NcSwitch(
            checked = false,
            onCheckedChange = {},
        )
    }
}
