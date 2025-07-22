package com.nunchuk.android.main.components.tabs.wallet.totalbalance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import com.nunchuk.android.utils.Utils

@Composable
fun TotalBalanceView(
    isLargeFont: Boolean = false,
    balanceSatoshis: String = "",
    balanceFiat: String = "",
    btcPrice: String = "",
    isHideBalance: Boolean = false
) {
    var showBalanceLocal by remember(isHideBalance) {
        mutableStateOf(isHideBalance)
    }

    val iconSize: Dp = if (isLargeFont) 24.dp else 20.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showBalanceLocal = !showBalanceLocal }
            .background(colorResource(id = R.color.nc_grey_light))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.nc_total_balance),
                    style = if (isLargeFont) NunchukTheme.typography.title else NunchukTheme.typography.titleSmall,
                )
                NcIcon(
                    painter = painterResource(id = com.nunchuk.android.widget.R.drawable.ic_chart_line),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(iconSize)
                )
            }
            Text(
                text = Utils.maskValue(balanceSatoshis, showBalanceLocal),
                style = if (isLargeFont) NunchukTheme.typography.title else NunchukTheme.typography.titleSmall,
                textAlign = TextAlign.End
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                text = "1BTC = $btcPrice",
                style = if (isLargeFont) NunchukTheme.typography.body else NunchukTheme.typography.bodySmall,
            )
            Text(
                text = "(${Utils.maskValue(balanceFiat, showBalanceLocal)})",
                style = if (isLargeFont) NunchukTheme.typography.body else NunchukTheme.typography.bodySmall,
                textAlign = TextAlign.End
            )
        }
    }
}

@PreviewLightDark
@Composable
fun TotalBalanceViewPreview() {
    NunchukTheme {
        TotalBalanceView(
            isLargeFont = true,
            balanceSatoshis = "21,134,277,620,930 sat",
            balanceFiat = "$5,540,000,000.00",
            btcPrice = "106,778.50",
            isHideBalance = false
        )
    }
}