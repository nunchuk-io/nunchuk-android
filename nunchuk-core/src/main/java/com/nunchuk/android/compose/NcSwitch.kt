package com.nunchuk.android.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
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
        checkedThumbColor = Color.White,
        uncheckedTrackColor = MaterialTheme.colorScheme.backgroundMidGray,
        checkedTrackColor = MaterialTheme.colorScheme.controlActivated,
        uncheckedThumbColor = Color.White,
        uncheckedBorderColor = MaterialTheme.colorScheme.backgroundMidGray,
        checkedBorderColor = MaterialTheme.colorScheme.controlActivated,
        disabledCheckedThumbColor = Color.White,
        disabledCheckedTrackColor = MaterialTheme.colorScheme.backgroundMidGray,
        disabledCheckedBorderColor = MaterialTheme.colorScheme.backgroundMidGray,
        disabledUncheckedThumbColor = MaterialTheme.colorScheme.backgroundMidGray.copy(alpha = 0.8f),
        disabledUncheckedTrackColor = Color.Transparent,
        disabledUncheckedBorderColor = MaterialTheme.colorScheme.backgroundMidGray.copy(alpha = 0.8f),
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
        Box(
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
        ) {
            NcSwitch(
                checked = true,
                onCheckedChange = {},
            )
        }
    }
}

@Composable
@PreviewLightDark
fun NcSwitchDefaultEnablePreview() {
    NunchukTheme {
        Box(
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
        ) {
            NcSwitch(
                checked = true,
                onCheckedChange = {},
                enabled = false
            )
        }
    }
}

@Composable
@PreviewLightDark
fun NcSwitchDefaultPreview() {
    NunchukTheme {
        Box(
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
        ) {
            NcSwitch(
                checked = false,
                enabled = false,
                onCheckedChange = {},
            )
        }
    }
}

@Composable
@PreviewLightDark
fun NcSwitchNotEnableFalsePreview() {
    NunchukTheme {
        Box(
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
        ) {
            NcSwitch(
                checked = false,
                onCheckedChange = {},
            )
        }
    }
}
