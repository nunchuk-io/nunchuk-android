package com.nunchuk.android.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NcRadioOption(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier, onClick = onClick,
        border = BorderStroke(
            width = 2.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.border
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(modifier = Modifier.padding(bottom = 16.dp, end = 16.dp), verticalAlignment = Alignment.Top) {
            RadioButton(modifier = Modifier.padding(top = 6.dp), selected = isSelected, onClick = onClick)
            Column(modifier = Modifier.padding(top = 16.dp)) {
                content()
            }
        }
    }
}

@Preview
@Composable
fun NcRadioOptionPreview() {
    NunchukTheme {
        NcRadioOption {
            Text(text = "A master account and secondary account(s) control three keys; Nunchuk holds the fourth key on our secure server to assist in inheritance planning and daily wallet operation.")
        }
    }
}