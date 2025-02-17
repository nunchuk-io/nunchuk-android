package com.nunchuk.android.main.components.tabs.wallet.totalbalance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import com.nunchuk.android.utils.Utils

@Composable
fun TotalBalanceView(
    isLargeFont: Boolean = false,
    balanceSatoshis: String = "",
    balanceDollars: String = "",
    isHideBalance: Boolean = false
) {
    var showBalanceLocal by remember(isHideBalance) {
        mutableStateOf(isHideBalance)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(id = R.color.nc_grey_light))
            .padding(16.dp)
            .clickable {
                showBalanceLocal = !showBalanceLocal
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.nc_total_balance),
            style = if (isLargeFont) NunchukTheme.typography.title else NunchukTheme.typography.titleSmall,
            modifier = Modifier.align(Alignment.CenterVertically)
        )

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = Utils.maskValue(
                    balanceSatoshis,
                    showBalanceLocal
                ),
                style = if (isLargeFont) NunchukTheme.typography.title else NunchukTheme.typography.titleSmall,
                textAlign = TextAlign.End
            )

            Text(
                text = Utils.maskValue(
                    balanceDollars,
                    showBalanceLocal
                ),
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
            balanceSatoshis = "0.00000000 BTC",
            balanceDollars = "$0.00"
        )
    }
}