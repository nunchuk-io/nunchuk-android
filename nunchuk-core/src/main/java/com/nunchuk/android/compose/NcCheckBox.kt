package com.nunchuk.android.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark

@Composable
fun NcCheckBox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors(
        uncheckedColor = MaterialTheme.colorScheme.controlDefault,
        checkedColor = MaterialTheme.colorScheme.controlActivated,
        checkmarkColor = MaterialTheme.colorScheme.controlTextPrimary
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource
    )
}

@PreviewLightDark
@Composable
private fun NcCheckBoxPreview() {
    NunchukTheme {
        Column(
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            NcCheckBox(checked = false, onCheckedChange = null)
            NcCheckBox(checked = true, onCheckedChange = null)
        }
    }
}