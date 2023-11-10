package com.nunchuk.android.main.membership.byzantine.payment.feerate

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioOption
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentViewModel
import com.nunchuk.android.model.FeeRate

@Composable
fun PaymentFeeRateRoute(
    recurringPaymentViewModel: RecurringPaymentViewModel,
    openPaymentCosignScreen: () -> Unit,
) {
    val state by recurringPaymentViewModel.config.collectAsStateWithLifecycle()
    PaymentFeeRateScreen(
        feeRate = state.feeRate,
        openPaymentCosignScreen = openPaymentCosignScreen,
        onFeeRateChange = recurringPaymentViewModel::onFeeRateChange,
    )
}

@Composable
private fun PaymentFeeRateScreen(
    feeRate: FeeRate = FeeRate.PRIORITY,
    openPaymentCosignScreen: () -> Unit = {},
    onFeeRateChange: (FeeRate) -> Unit = {},
) {
    NunchukTheme {
        Scaffold(topBar = {
            NcTopAppBar(
                title = stringResource(R.string.nc_add_recurring_payments),
                textStyle = NunchukTheme.typography.titleLarge,
            )
        }, bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                onClick = openPaymentCosignScreen,
            ) {
                Text(text = stringResource(R.string.nc_text_continue))
            }
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(R.string.nc_payment_fee_rate_title),
                    style = NunchukTheme.typography.body,
                )

                FeeRate.values().forEach {
                    PaymentFeeRateOption(
                        modifier = Modifier.padding(top = 16.dp),
                        isSelected = it == feeRate,
                        type = it,
                        onClick = { onFeeRateChange(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentFeeRateOption(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    type: FeeRate,
    onClick: () -> Unit = {},
) {
    NcRadioOption(modifier = modifier.fillMaxWidth(), isSelected = isSelected, onClick = onClick) {
        Text(text = stringResource(id = type.toTitle()), style = NunchukTheme.typography.title)
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = stringResource(id = type.toDesc()),
            style = NunchukTheme.typography.body
        )
    }
}

@StringRes
fun FeeRate.toTitle() = when (this) {
    FeeRate.PRIORITY -> R.string.nc_priority_fee_rate
    FeeRate.STANDARD -> R.string.nc_standard_fee_rate
    FeeRate.ECONOMY -> R.string.nc_economy_fee_rate
}

@StringRes
fun FeeRate.toDesc() = when (this) {
    FeeRate.PRIORITY -> R.string.nc_priority_fee_rate_desc
    FeeRate.STANDARD -> R.string.nc_standard_fee_rate_desc
    FeeRate.ECONOMY -> R.string.nc_economy_fee_rate_desc
}

@Preview
@Composable
fun PaymentFeeRateScreenPreview() {
    PaymentFeeRateScreen()
}