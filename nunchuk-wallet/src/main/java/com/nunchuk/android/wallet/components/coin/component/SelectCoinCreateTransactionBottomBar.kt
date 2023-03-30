package com.nunchuk.android.wallet.components.coin.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.wallet.R

@Composable
fun SelectCoinCreateTransactionBottomBar(
    amount: Amount = Amount(),
    selectedCoin: Set<UnspentOutput> = emptySet(),
    onUseCoinClicked: () -> Unit = {},
) {
    val total = selectedCoin.sumOf { it.amount.value }
    Column {
        Text(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 8.dp)
                .align(alignment = Alignment.CenterHorizontally),
            text = stringResource(R.string.nc_selected, Amount(total).getBTCAmount()),
            style = NunchukTheme.typography.titleSmall,
        )
        Text(
            modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
            text = stringResource(R.string.nc_please_select_at_least, amount.getBTCAmount()),
            style = NunchukTheme.typography.bodySmall,
        )
        NcPrimaryDarkButton(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            onClick = onUseCoinClicked,
            enabled = total >= amount.value
        ) {
            Text(text = stringResource(R.string.nc_use_selected_coins))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SelectCoinCreateTransactionBottomBarPreview() {
    NunchukTheme {
        Column {
            SelectCoinCreateTransactionBottomBar()
        }
    }
}