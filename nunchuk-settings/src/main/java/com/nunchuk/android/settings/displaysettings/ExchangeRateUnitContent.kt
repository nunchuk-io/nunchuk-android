package com.nunchuk.android.settings.displaysettings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcRadioButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.domain.data.BTC
import com.nunchuk.android.core.domain.data.SAT
import com.nunchuk.android.settings.R

@Composable
fun ExchangeRateUnitContent(
    currentUnit: Int = BTC,
    onUnitSelected: (Int) -> Unit = { }
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier.statusBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(id = R.string.nc_exchange_rate_unit),
                    textStyle = NunchukTheme.typography.titleLarge
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .fillMaxHeight()
            ) {
                Row(
                    modifier = Modifier
                        .clickable { onUnitSelected(BTC) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier
                            .weight(1f),
                        text = stringResource(R.string.nc_currency_btc),
                        style = NunchukTheme.typography.body
                    )
                    NcRadioButton(
                        selected = currentUnit == BTC,
                        onClick = { onUnitSelected(BTC) }
                    )
                }

                Row(
                    modifier = Modifier
                        .clickable { onUnitSelected(SAT) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier
                            .weight(1f),
                        text = stringResource(R.string.nc_currency_sat),
                        style = NunchukTheme.typography.body
                    )
                    NcRadioButton(
                        selected = currentUnit == SAT,
                        onClick = { onUnitSelected(SAT) }
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
fun ExchangeRateUnitContentPreview() {
    ExchangeRateUnitContent(currentUnit = BTC)
}