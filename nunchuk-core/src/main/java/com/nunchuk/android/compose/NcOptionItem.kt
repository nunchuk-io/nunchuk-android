package com.nunchuk.android.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp

@Composable
fun NcOptionItem(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(), onClick = onClick,
        border = BorderStroke(
            width = 2.dp,
            color = if (isSelected) MaterialTheme.colorScheme.textPrimary else MaterialTheme.colorScheme.strokePrimary
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            NcRadioButton(modifier = Modifier.size(24.dp), selected = isSelected, onClick = onClick)
            Text(
                modifier = Modifier.padding(start = 12.dp),
                text = label,
                style = NunchukTheme.typography.title
            )
        }
    }
}

@PreviewLightDark
@Composable
fun PreviewOptionItem() {
    NunchukTheme {
        Column {
            NcOptionItem(
                isSelected = true,
                label = "Option Item",
                onClick = {}
            )
            Spacer(Modifier.height(12.dp))
            NcOptionItem(
                isSelected = false,
                label = "Option Item",
                onClick = {}
            )
        }

    }
}