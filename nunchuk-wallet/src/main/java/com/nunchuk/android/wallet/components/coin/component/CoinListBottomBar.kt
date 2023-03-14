package com.nunchuk.android.wallet.components.coin.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.coin.CoinCard
import com.nunchuk.android.model.coin.CoinTag
import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.wallet.R

@Composable
fun CoinListBottomBar(
    modifier: Modifier = Modifier,
    selectedCoin: Set<CoinCard>,
    onSendBtc: () -> Unit = {},
    onShowSelectedCoinMoreOption: () -> Unit = {}
) {
    val total = selectedCoin.sumOf { it.amount.value }
    Row(
        modifier = modifier
            .padding(vertical = 16.dp)
            .fillMaxWidth()
    ) {
        IconButton(onClick = onSendBtc) {
            Icon(
                painter = painterResource(id = R.drawable.ic_sending_bitcoin),
                contentDescription = "Send Btc"
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.nc_number_of_coin_selected, selectedCoin.size),
                style = NunchukTheme.typography.title,
            )
            Text(
                text = if (LocalView.current.isInEditMode) "200,000 sats" else Amount(total).getBTCAmount(),
                style = NunchukTheme.typography.bodySmall,
            )
        }
        IconButton(onClick = onShowSelectedCoinMoreOption) {
            Icon(
                painter = painterResource(id = R.drawable.ic_more),
                contentDescription = "Menu more"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CoinListBottomBarPreview() {
    NunchukTheme {
        CoinListBottomBar(
            selectedCoin = setOf(
                CoinCard(
                    amount = Amount(1000000L),
                    isLocked = true,
                    isScheduleBroadCast = true,
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
        )
    }
}