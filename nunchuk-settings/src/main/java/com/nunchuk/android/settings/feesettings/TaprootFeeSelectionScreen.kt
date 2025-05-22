package com.nunchuk.android.settings.feesettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcNumberInputField
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcSwitch
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.showNunchukSnackbar
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.R
import com.nunchuk.android.core.util.CurrencyFormatter
import kotlinx.coroutines.launch

@Composable
fun TaprootFeeSelectionContent(
    state: FeeSettingsState = FeeSettingsState(),
    onContinueClick: (
        automaticFeeEnabled: Boolean,
        taprootPercentage: String,
        taprootAmount: String,
    ) -> Unit = { _, _, _ -> },
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackState = remember { SnackbarHostState() }
    var automaticFeeEnabled by remember(state.automaticFee) {
        mutableStateOf(state.automaticFee)
    }

    var taprootPercentage by remember(state.taprootPercentage) {
        mutableStateOf(state.taprootPercentage)
    }
    var taprootAmount by remember(state.taprootAmount) {
        mutableStateOf(state.taprootAmount)
    }

    val taprootPercentageVal = taprootPercentage.toIntOrNull() ?: 0
    val taprootAmountVal = taprootAmount.toDoubleOrNull() ?: 0.0

    NunchukTheme {
        NcScaffold(
            modifier = Modifier
                .navigationBarsPadding()
                .statusBarsPadding(),
            snackState = snackState,
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
                        onClick = {
                            if (taprootAmountVal <= 0.0 || taprootPercentageVal <= 0) {
                                coroutineScope.launch {
                                    snackState.showNunchukSnackbar(
                                        message = context.getString(com.nunchuk.android.settings.R.string.nc_threshold_must_be_greater_than_0),
                                        type = NcToastType.ERROR
                                    )
                                }
                            } else {
                                onContinueClick(
                                    automaticFeeEnabled,
                                    taprootPercentage,
                                    taprootAmount
                                )
                            }
                        },
                    ) {
                        Text(text = stringResource(R.string.nc_save))
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f, true)
                            .padding(end = 12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.nc_automatic_fee),
                            style = NunchukTheme.typography.body
                        )
                        Text(
                            modifier = Modifier.padding(top = 4.dp),
                            text = stringResource(R.string.nc_automatic_fee_desc),
                            style = NunchukTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.textSecondary)
                        )
                    }

                    NcSwitch(
                        checked = automaticFeeEnabled,
                        onCheckedChange = {
                            automaticFeeEnabled = it
                        },
                    )
                }

                if (automaticFeeEnabled) {
                    NcNumberInputField(
                        modifier = Modifier
                            .padding(top = 16.dp),
                        title = stringResource(R.string.nc_fee_difference_threshold_percent),
                        value = taprootPercentage,
                        onValueChange = { s ->
                            val numberOfDigit = 4
                            val format = CurrencyFormatter.format(s, numberOfDigit)
                            taprootPercentage = if ((format.toDoubleOrNull() ?: 0.0) > 100.0) {
                                "100"
                            } else {
                                format
                            }
                        },
                        suffix = "%",
                    )

                    NcNumberInputField(
                        modifier = Modifier
                            .padding(top = 16.dp),
                        title = stringResource(
                            R.string.nc_fee_difference_threshold_currency,
                            "USD"
                        ),
                        value = taprootAmount,
                        onValueChange = { s ->
                            taprootAmount = CurrencyFormatter.format(s, 2)
                        },
                        suffix = "",
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
fun TaprootFeeSelectionContentPreview() {
    TaprootFeeSelectionContent(
        state = FeeSettingsState(
            automaticFee = true,
            taprootPercentage = "10",
            taprootAmount = "0.01"
        ),
    )
}