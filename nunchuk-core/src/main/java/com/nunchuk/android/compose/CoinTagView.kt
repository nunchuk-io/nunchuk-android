package com.nunchuk.android.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.nunchuk.android.model.CoinTag

@Composable
fun CoinTagView(
    modifier: Modifier = Modifier,
    textStyle: TextStyle = NunchukTheme.typography.bodySmall,
    circleSize: Dp = 16.dp,
    tag: CoinTag,
    clickable: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier
            .background(
                color = NcColor.greyLight, shape = RoundedCornerShape(24.dp)
            ).let {
                if (clickable) {
                    it.clickable(onClick = onClick)
                } else it
            }
            .border(1.dp, color = NcColor.whisper, shape = RoundedCornerShape(24.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(circleSize)
                .clip(CircleShape)
                .background(Color(tag.color.toColorInt()))
        )
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = if (tag.name.length < 20) tag.name else "${tag.name.take(20)}...",
            style = textStyle,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CoinTagViewPreview() {
    NunchukTheme {
        CoinTagView(
            tag = CoinTag(
                id = 1, color = "blue", name = "Kidding"
            )
        )
    }
}