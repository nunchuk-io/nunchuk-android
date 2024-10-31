package com.nunchuk.android.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun NcTextSwitch(
    modifier: Modifier = Modifier,
    title: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = NunchukTheme.typography.body
        )
        NcSwitch(
            checked = value,
            onCheckedChange = onValueChange,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NcTextSwitchPreview() {
    NunchukTheme {
        NcTextSwitch(
            modifier = Modifier,
            title = "Title",
            value = true,
            onValueChange = {}
        )
    }
}