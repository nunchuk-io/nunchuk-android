package com.nunchuk.android.wallet.components.coin.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunchuk.android.compose.NcColor
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.coin.CoinCard
import com.nunchuk.android.model.coin.CoinTag
import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.utils.formatByHour
import com.nunchuk.android.utils.simpleDateFormat
import com.nunchuk.android.wallet.R
import java.util.*

@Composable
fun PreviewCoinCard(
    coinCard: CoinCard,
    selectable: Boolean = false,
    isSelected: Boolean = false,
    onViewCoinDetail: (coinCard: CoinCard) -> Unit = {},
    onSelectCoin: (coinCard: CoinCard, isSelected: Boolean) -> Unit = { _, _ -> }
) {
    Box(modifier = Modifier.clickable { onViewCoinDetail(coinCard) }) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (LocalView.current.isInEditMode)
                        "${coinCard.amount.value} sats"
                    else
                        coinCard.amount.getBTCAmount(),
                    style = NunchukTheme.typography.title
                )
                if (coinCard.isChange) {
                    Text(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .background(
                                color = MaterialTheme.colors.background,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .border(
                                1.dp,
                                color = NcColor.whisper,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        text = stringResource(R.string.nc_change),
                        style = NunchukTheme.typography.titleSmall.copy(fontSize = 10.sp)
                    )
                }
                if (coinCard.isLocked) {
                    Icon(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .background(
                                color = colorResource(id = R.color.nc_whisper_color),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .size(12.dp),
                        painter = painterResource(id = R.drawable.ic_lock),
                        contentDescription = "Lock"
                    )
                }
                if (coinCard.isScheduleBroadCast) {
                    Icon(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .background(
                                color = colorResource(id = R.color.nc_whisper_color),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .size(12.dp),
                        painter = painterResource(id = R.drawable.ic_schedule),
                        contentDescription = "Schedule"
                    )
                }
            }
            if (coinCard.time > 0L) {
                val date = Date(coinCard.time)
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = "${date.simpleDateFormat()} at ${date.formatByHour()}",
                    style = NunchukTheme.typography.bodySmall
                )
            } else {
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = "--/--/--",
                    style = NunchukTheme.typography.bodySmall
                )
            }

            if (coinCard.tags.isNotEmpty() || coinCard.note.isNotEmpty()) {
                CoinTagGroupView(
                    Modifier.padding(top = 4.dp), note = coinCard.note, tags = coinCard.tags
                )
            }
        }
        if (selectable) {
            Checkbox(modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp),
                checked = isSelected,
                onCheckedChange = { select ->
                    onSelectCoin(coinCard, select)
                })
        } else {
            IconButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp),
                onClick = { onViewCoinDetail(coinCard) }) {
                Icon(painter = painterResource(id = R.drawable.ic_arrow), contentDescription = "")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCoinCardPreview() {
    NunchukTheme {
        PreviewCoinCard(
            coinCard = CoinCard(
                amount = Amount(1000000L),
                isLocked = true,
                isScheduleBroadCast = true,
                isChange = true,
                time = System.currentTimeMillis(),
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
                ),
                note = "Send to Bob on Silk Road",
                status = TransactionStatus.PENDING_CONFIRMATION
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
                amount = Amount(1000000L),
                isLocked = false,
                isScheduleBroadCast = true,
                time = System.currentTimeMillis(),
                tags = listOf(),
                note = "",
                status = TransactionStatus.PENDING_CONFIRMATION
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCoinCardPreview3() {
    NunchukTheme {
        PreviewCoinCard(
            coinCard = CoinCard(
                amount = Amount(1000000L),
                isLocked = false,
                isScheduleBroadCast = true,
                time = System.currentTimeMillis(),
                tags = listOf(),
                note = "",
                status = TransactionStatus.PENDING_CONFIRMATION
            ),
            selectable = true,
            isSelected = true
        )
    }
}