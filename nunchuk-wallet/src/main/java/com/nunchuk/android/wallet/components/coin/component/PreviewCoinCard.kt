package com.nunchuk.android.wallet.components.coin.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.Badge
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunchuk.android.compose.NcBadge
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.model.coin.CoinCard
import com.nunchuk.android.utils.formatByHour
import com.nunchuk.android.utils.simpleDateFormat
import com.nunchuk.android.wallet.R
import java.util.*

@Composable
fun PreviewCoinCard(coinCard: CoinCard) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = coinCard.amount, style = NunchukTheme.typography.title)
            if (coinCard.isLock) {
                NcBadge(
                    modifier = Modifier.padding(start = 4.dp),
                    backgroundColor = colorResource(id = R.color.nc_whisper_color)
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(12.dp),
                        painter = painterResource(id = R.drawable.ic_lock),
                        contentDescription = "Lock"
                    )
                }
            } else {
                Badge(
                    modifier = Modifier.padding(start = 4.dp),
                    backgroundColor = MaterialTheme.colors.background
                ) {
                    Text(
                        modifier = Modifier.padding(2.dp),
                        text = "Change",
                        style = NunchukTheme.typography.titleSmall.copy(fontSize = 10.sp)
                    )
                }
            }
            if (coinCard.isScheduleBroadCast) {
                Badge(
                    modifier = Modifier.padding(start = 4.dp),
                    backgroundColor = colorResource(id = R.color.nc_whisper_color)
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(12.dp),
                        painter = painterResource(id = R.drawable.ic_schedule),
                        contentDescription = "Lock"
                    )
                }
            }
        }
        val date = Date(coinCard.time)
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = "${date.simpleDateFormat()} at ${date.formatByHour()}",
            style = NunchukTheme.typography.bodySmall
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCoinCardPreview() {
    NunchukTheme {
        PreviewCoinCard(
            coinCard = CoinCard(
                amount = "100,000 sats",
                isLock = true,
                isScheduleBroadCast = true,
                time = System.currentTimeMillis(),
                tags = listOf()
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCoinCardPreview2() {
    NunchukTheme {
        PreviewCoinCard(
            coinCard = CoinCard(
                amount = "100,000 sats",
                isLock = false,
                isScheduleBroadCast = true,
                time = System.currentTimeMillis(),
                tags = listOf()
            )
        )
    }
}