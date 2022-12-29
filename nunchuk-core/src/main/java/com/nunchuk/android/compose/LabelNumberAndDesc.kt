package com.nunchuk.android.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun LabelNumberAndDesc(
    modifier: Modifier = Modifier,
    index: Int,
    title: String,
    titleStyle: TextStyle = NunchukTheme.typography.body,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        NCLabelWithIndex(index = index, label = title, style = titleStyle)
        content()
    }
}

@Preview
@Composable
private fun LabelNumberAndDescPreview() {
    NunchukTheme {
        Box(modifier = Modifier.background(Color.White)) {
            LabelNumberAndDesc(index = 1, title = "Unlock COLDCARD") {
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = "You might need to enter a PIN and/or a PASSPHRASE directly on COLDCARD.",
                    style = NunchukTheme.typography.body
                )
            }
        }
    }
}