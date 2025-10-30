package com.nunchuk.android.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NcRadioButtonOption(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    showRadioButton: Boolean = true,
    customBackgroundColor: Color? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
        border = BorderStroke(
            width = 2.dp,
            color = if (isSelected) MaterialTheme.colorScheme.textPrimary else MaterialTheme.colorScheme.strokePrimary
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = customBackgroundColor ?: MaterialTheme.colorScheme.background,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.4f)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(16.dp),
        ) {
            if (showRadioButton) {
                NcRadioButton(
                    modifier = Modifier.size(24.dp).align(Alignment.TopEnd),
                    selected = isSelected,
                    enabled = enabled,
                    onClick = if (enabled) onClick else null
                )
            }
            content()
        }
    }
}

@Preview
@Composable
fun NcRadioButtonOptionPreview() {
    NunchukTheme {
        NcRadioButtonOption {
            Text(text = "A master account and secondary account(s) control three keys; Nunchuk holds the fourth key on our secure server to assist in inheritance planning and daily wallet operation.")
        }
    }
}

@Preview
@Composable
fun NcRadioButtonOptionSelectedPreview() {
    NunchukTheme {
        NcRadioButtonOption(isSelected = true) {
            Text(text = "A master account and secondary account(s) control three keys; Nunchuk holds the fourth key on our secure server to assist in inheritance planning and daily wallet operation.")
        }
    }
}

@Preview
@Composable
fun NcRadioButtonOptionDisablePreview() {
    NunchukTheme {
        NcRadioButtonOption(enabled = false) {
            Text(text = "A master account and secondary account(s) control three keys; Nunchuk holds the fourth key on our secure server to assist in inheritance planning and daily wallet operation.")
        }
    }
}
