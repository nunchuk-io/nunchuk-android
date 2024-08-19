package com.nunchuk.android.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NcCheckBoxOption(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        enabled = enabled,
        onClick = { onCheckedChange(!isSelected) },
        border = BorderStroke(
            width = 2.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.border
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.4f)
                .clickable(enabled = enabled, onClick = { onCheckedChange(!isSelected) })
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                Checkbox(
                    modifier = Modifier.padding(),
                    checked = isSelected,
                    enabled = enabled,
                    onCheckedChange = onCheckedChange
                )
            }
            Column(
                modifier = Modifier.align(alignment = Alignment.CenterVertically),
            ) {
                content()
            }
        }
    }
}

@Preview
@Composable
fun NcCheckBoxOptionPreview() {
    NunchukTheme {
        NcCheckBoxOption {
            Text(text = "A master account and secondary account(s) control three keys; Nunchuk holds the fourth key on our secure server to assist in inheritance planning and daily wallet operation.")
        }
    }
}

@Preview
@Composable
fun NcCheckBoxOptionDisablePreview() {
    NunchukTheme {
        NcCheckBoxOption(enabled = false) {
            Text(text = "A master account and secondary account(s) control three keys; Nunchuk holds the fourth key on our secure server to assist in inheritance planning and daily wallet operation.")
        }
    }
}