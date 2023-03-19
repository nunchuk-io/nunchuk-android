package com.nunchuk.android.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.nunchuk.android.model.CoinTag

@Composable
fun CoinTagView(modifier: Modifier = Modifier, tag: CoinTag) {
    Row(
        modifier
            .background(
                color = NcColor.greyLight,
                shape = RoundedCornerShape(24.dp)
            )
            .border(1.dp, color = NcColor.whisper, shape = RoundedCornerShape(24.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(Color(tag.color.toColorInt()))
        )
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = "#${tag.name}",
            style = NunchukTheme.typography.bodySmall,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CoinTagViewPreview() {
    NunchukTheme {
        CoinTagView(
            tag = CoinTag(
                id = 1,
                color = "blue",
                name = "Kidding"
            )
        )
    }
}