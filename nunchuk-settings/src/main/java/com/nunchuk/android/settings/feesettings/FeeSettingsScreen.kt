package com.nunchuk.android.settings.feesettings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSwitch
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.R
import com.nunchuk.android.model.FreeRateOption

@Composable
fun FeeSettingsContent(
    feeRate: Int = FreeRateOption.ECONOMIC.ordinal,
    defaultAntiFeeSnipingEnabled: Boolean = false,
    onContinueClick: (antiFeeSniping: Boolean) -> Unit = { _ -> },
    openTaprootFeeSelection: () -> Unit = { },
    openDefaultFeeRate: () -> Unit = { },
) {
    var antiFeeSnipingEnabled by remember(defaultAntiFeeSnipingEnabled) {
        mutableStateOf(defaultAntiFeeSnipingEnabled)
    }

    val feeRateText = when (feeRate) {
        FreeRateOption.ECONOMIC.ordinal -> stringResource(R.string.nc_economy)
        FreeRateOption.STANDARD.ordinal -> stringResource(R.string.nc_standard_option)
        FreeRateOption.PRIORITY.ordinal -> stringResource(R.string.nc_priority)
        else -> stringResource(R.string.nc_economy)
    }

    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .navigationBarsPadding()
                .statusBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_fee_settings),
                    textStyle = NunchukTheme.typography.titleLarge
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth(),
                        enabled = antiFeeSnipingEnabled != defaultAntiFeeSnipingEnabled,
                        onClick = {
                            onContinueClick(antiFeeSnipingEnabled)
                        }) {
                        Text(text = stringResource(R.string.nc_save_fee_settings))
                    }
                }
            },
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
                        .clickable { openDefaultFeeRate() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.nc_default_fee_rate),
                        style = NunchukTheme.typography.body,
                        modifier = Modifier.weight(1f, true)
                    )

                    Text(
                        text = feeRateText,
                        style = NunchukTheme.typography.body,
                        color = MaterialTheme.colorScheme.textSecondary,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    NcIcon(
                        modifier = Modifier,
                        painter = painterResource(R.drawable.ic_right_arrow_dark),
                        contentDescription = null,
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 28.dp).clickable {
                        openTaprootFeeSelection()
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f, true)
                            .padding(end = 12.dp),
                    ) {
                        Text(
                            text = "Automatic fee selection",
                            style = NunchukTheme.typography.body
                        )
                        Text(
                            modifier = Modifier.padding(top = 4.dp),
                            text = "Applies when a transaction has multiple signing policies with different fees",
                            style = NunchukTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.textSecondary)
                        )
                    }
                    NcIcon(
                        modifier = Modifier,
                        painter = painterResource(R.drawable.ic_right_arrow_dark),
                        contentDescription = null,
                    )

                }

                Row(
                    modifier = Modifier.padding(top = 28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f, true)
                            .padding(end = 12.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.nc_enable_anti_fee_sniping_by_default),
                            style = NunchukTheme.typography.body
                        )
                        Text(
                            modifier = Modifier.padding(top = 4.dp),
                            text = stringResource(id = R.string.nc_enable_anti_fee_sniping_by_default_desc),
                            style = NunchukTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.textSecondary)
                        )
                    }

                    NcSwitch(
                        checked = antiFeeSnipingEnabled,
                        onCheckedChange = {
                            antiFeeSnipingEnabled = it
                        },
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
fun FeeSettingsContentPreview() {
    FeeSettingsContent()
}