package com.nunchuk.android.settings.displaysettings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcSwitch
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.greyDark
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.domain.data.SAT
import com.nunchuk.android.settings.R

@Composable
fun HomeScreenSettingsContent(
    uiState: DisplaySettingsUiState = DisplaySettingsUiState(),
    onFontSizeChange: (Boolean) -> Unit = { },
    onDisplayTotalBalanceChange: (Boolean) -> Unit = { },
    onDisplayWalletShortcutChange: (Boolean) -> Unit = { },
    onExchangeRateUnitClick: () -> Unit = { }
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier.statusBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(id = R.string.nc_home_screen_settings),
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
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier
                            .weight(1f, true)
                            .padding(end = 12.dp),
                        text = stringResource(id = R.string.nc_use_large_font_balances_home_screen),
                        style = NunchukTheme.typography.body
                    )

                    NcSwitch(
                        checked = uiState.homeDisplaySetting.useLargeFont,
                        onCheckedChange = onFontSizeChange,
                    )
                }

                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier
                            .weight(1f, true)
                            .padding(end = 12.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.nc_display_wallet_shortcut),
                            style = NunchukTheme.typography.body
                        )
                        Text(
                            modifier = Modifier.padding(top = 4.dp),
                            text = stringResource(id = R.string.nc_display_wallet_shortcut_desc),
                            style = NunchukTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.greyDark)
                        )
                    }

                    NcSwitch(
                        checked = uiState.homeDisplaySetting.showWalletShortcuts,
                        onCheckedChange = onDisplayWalletShortcutChange,
                    )
                }

                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier
                            .weight(1f, true)
                            .padding(end = 12.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.nc_display_total_balance_home_screen),
                            style = NunchukTheme.typography.body
                        )
                    }

                    NcSwitch(
                        checked = uiState.homeDisplaySetting.showTotalBalance,
                        onCheckedChange = onDisplayTotalBalanceChange,
                    )
                }

                Row(
                    modifier = Modifier
                        .clickable { onExchangeRateUnitClick() }
                        .padding(top = 12.dp, bottom = 16.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.nc_exchange_rate_unit),
                        style = NunchukTheme.typography.body
                    )
                    val unitText = if (uiState.homeDisplaySetting.exchangeRateUnit == SAT) {
                        stringResource(R.string.nc_currency_sat)
                    } else {
                        stringResource(R.string.nc_currency_btc)
                    }
                    Text(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .weight(1f, true),
                        text = "($unitText)",
                        style = NunchukTheme.typography.body.copy(color = MaterialTheme.colorScheme.textSecondary)
                    )

                    NcIcon(
                        painter = painterResource(id = R.drawable.ic_arrow),
                        contentDescription = ""
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
fun HomeScreenSettingsContentPreview() {
    HomeScreenSettingsContent()
}