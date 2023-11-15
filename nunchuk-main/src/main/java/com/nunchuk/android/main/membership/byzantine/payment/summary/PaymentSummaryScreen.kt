package com.nunchuk.android.main.membership.byzantine.payment.summary

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentViewModel
import com.nunchuk.android.model.FeeRate
import com.nunchuk.android.model.payment.PaymentFrequency

@Composable
fun PaymentSummaryRoute(
    recurringPaymentViewModel: RecurringPaymentViewModel,
) {
    val state by recurringPaymentViewModel.config.collectAsStateWithLifecycle()
    PaymentSummaryScreen(
        isCosign = state.isCosign == true,
        name = state.name,
        amount = state.amount,
        frequency = state.frequency ?: PaymentFrequency.DAILY,
        startDate = state.startDate,
        endDate = state.endDate,
        noEndDate = state.noEndDate,
        feeRate = state.feeRate,
        addresses = state.addresses,
        note = state.note,
        onSubmit = recurringPaymentViewModel::onSubmit,
    )
}

@Composable
fun PaymentSummaryScreen(
    onSubmit: () -> Unit = {},
    isCosign: Boolean = false,
    name: String = "",
    amount: String = "",
    frequency: PaymentFrequency = PaymentFrequency.DAILY,
    startDate: Long = 0,
    endDate: Long = 0,
    noEndDate: Boolean = false,
    feeRate: FeeRate = FeeRate.PRIORITY,
    addresses: List<String> = emptyList(),
    note: String = "",
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
                onClick = onSubmit,
            ) {
                Text(text = stringResource(R.string.nc_text_continue))
            }
        }) { innerPadding ->
            PaymentSummaryContent(
                modifier = Modifier.padding(innerPadding),
                isCosign = isCosign,
                name = name,
                amount = amount,
                frequency = frequency,
                startDate = startDate,
                noEndDate = noEndDate,
                endDate = endDate,
                feeRate = feeRate,
                addresses = addresses,
                note = note
            )
        }
    }
}

@Preview
@Composable
fun PaymentSummaryScreenPreview() {
    PaymentSummaryScreen(
        isCosign = true,
        name = "John Doe",
        amount = "100",
        frequency = PaymentFrequency.DAILY,
        startDate = 0,
        endDate = 0,
        noEndDate = false,
        feeRate = FeeRate.PRIORITY,
        addresses = emptyList(),
        note = "",
    )
}
