package com.nunchuk.android.wallet.components.coin.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcColor
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.model.coin.CoinTag
import com.nunchuk.android.wallet.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoinTagGroupView(modifier: Modifier = Modifier, note: String, tags: List<CoinTag>) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, NcColor.border, RoundedCornerShape(12.dp))
    ) {
        FlowRow(
            Modifier
                .fillMaxWidth()
                .padding(4.dp)) {
            tags.forEach {
                CoinTagView(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp), tag = it
                )
            }
        }
        Row(
            modifier = Modifier.padding(bottom = if (note.isNotEmpty()) 8.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (note.isNotEmpty()) {
                Icon(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .border(1.dp, color = NcColor.border, shape = CircleShape)
                        .padding(4.dp),
                    painter = painterResource(id = R.drawable.ic_transaction_note),
                    contentDescription = "Transaction Note"
                )
                Text(
                    modifier = Modifier.padding(start = 4.dp),
                    text = note,
                    style = NunchukTheme.typography.bodySmall
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CoinTagGroupView() {
    NunchukTheme {
        CoinTagGroupView(
            modifier = Modifier.padding(16.dp),
            tags = listOf(
                CoinTag(Color.Blue.toArgb(), "Badcoins"),
                CoinTag(Color.Red.toArgb(), "Dirtycoins"),
                CoinTag(Color.Gray.toArgb(), "Dirty"),
                CoinTag(Color.Green.toArgb(), "Dirtys"),
                CoinTag(Color.DarkGray.toArgb(), "Dirtycoins"),
                CoinTag(Color.LightGray.toArgb(), "Dirtycoins"),
                CoinTag(Color.Magenta.toArgb(), "Dirtycoins"),
                CoinTag(Color.Cyan.toArgb(), "Dirtycoins"),
                CoinTag(Color.Black.toArgb(), "Dirtycoins"),
            ), note = "Send to Bob on Silk Road"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CoinTagGroupViewNoNote() {
    NunchukTheme {
        CoinTagGroupView(
            modifier = Modifier.padding(16.dp),
            tags = listOf(
                CoinTag(Color.Blue.toArgb(), "Badcoins"),
                CoinTag(Color.Red.toArgb(), "Dirtycoins"),
                CoinTag(Color.Gray.toArgb(), "Dirty"),
                CoinTag(Color.Green.toArgb(), "Dirtys"),
                CoinTag(Color.DarkGray.toArgb(), "Dirtycoins"),
                CoinTag(Color.LightGray.toArgb(), "Dirtycoins"),
                CoinTag(Color.Magenta.toArgb(), "Dirtycoins"),
                CoinTag(Color.Cyan.toArgb(), "Dirtycoins"),
                CoinTag(Color.Black.toArgb(), "Dirtycoins"),
            ), note = ""
        )
    }
}