package com.nunchuk.android.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun NCLabelWithIndex(modifier: Modifier = Modifier, index: Int, label: String) {
    Row(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(width = 2.dp, color = MaterialTheme.colors.primary, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$index",
                style = NunchukTheme.typography.titleSmall.copy(fontWeight = FontWeight.W900)
            )
        }
        Text(
            modifier = Modifier.padding(start = 12.dp),
            text = label,
            style = NunchukTheme.typography.body
        )
    }
}

@Preview
@Composable
fun NcLabelWithIndexPreview() {
    NunchukTheme {
        NCLabelWithIndex(
            index = 1,
            label = "Use any TAPSIGNER-compatible tool to ensure that you can decrypt the backup via the decryption key printed on the back of the TAPSIGNER."
        )
    }
}