package com.nunchuk.android.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SelectableContainer(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(16.dp),
    borderWidth: Dp = 1.dp,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    content: @Composable () -> Unit = { }
) {
    Box(
        modifier = modifier
            .clickable(enabled = true, onClick = onClick)
            .border(
                width = borderWidth,
                color = if (isSelected) MaterialTheme.colorScheme.textPrimary else MaterialTheme.colorScheme.strokePrimary,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(paddingValues)
            .background(MaterialTheme.colorScheme.background)
    ) {
        content()
    }
}

@Composable
@PreviewLightDark
fun SelectableContainerPreview() {
    NunchukTheme {
        Column(
            modifier = Modifier.background(
                MaterialTheme.colorScheme.background,
            )
        ) {
            SelectableContainer(
                isSelected = false,
                paddingValues = PaddingValues(16.dp),
                onClick = {}
            ) {
                Text(text = "Selected", style = NunchukTheme.typography.title)
            }
            SelectableContainer(
                isSelected = false,
                paddingValues = PaddingValues(16.dp),
                onClick = {}
            ) {
                Text(text = "Not Selected", style = NunchukTheme.typography.title)
            }
        }
    }
}