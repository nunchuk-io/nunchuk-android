package com.nunchuk.android.wallet.components.coin.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcColor
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.model.coin.CoinTag

@Composable
fun CoinTagView(modifier: Modifier = Modifier, tag: CoinTag) {
    Row(
        modifier
            .background(
                color = MaterialTheme.colors.background,
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
                .background(Color(tag.color))
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
                Color.Blue.toArgb(),
                "Kidding"
            )
        )
    }
}