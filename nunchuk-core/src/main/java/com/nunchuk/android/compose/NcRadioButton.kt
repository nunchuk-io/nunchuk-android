package com.nunchuk.android.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark

@Composable
fun NcRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: RadioButtonColors = RadioButtonDefaults.colors(
        selectedColor = MaterialTheme.colorScheme.controlActivated,
        unselectedColor = MaterialTheme.colorScheme.controlDefault
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    RadioButton(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource
    )
}

@PreviewLightDark
@Composable
private fun NcRadioButtonPreview() {
    NunchukTheme {
        Column(
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            NcRadioButton(selected = false, onClick = null)
            NcRadioButton(selected = true, onClick = null)
        }
    }
}