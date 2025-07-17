package com.nunchuk.android.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun NcFilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedBackgroundColor: Color = MaterialTheme.colorScheme.controlActivated,
    selectedTextColor: Color = MaterialTheme.colorScheme.controlTextPrimary,
    unselectedBackgroundColor: Color = MaterialTheme.colorScheme.lightGray,
    unselectedTextColor: Color = MaterialTheme.colorScheme.textPrimary,
    unselectedBorderColor: Color = MaterialTheme.colorScheme.strokePrimary
) {
    Box(
        modifier = modifier
            .background(
                color = if (isSelected) selectedBackgroundColor else unselectedBackgroundColor,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = if (isSelected) Color.Transparent else unselectedBorderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = NunchukTheme.typography.bodySmall,
            color = if (isSelected) selectedTextColor else unselectedTextColor
        )
    }
} 