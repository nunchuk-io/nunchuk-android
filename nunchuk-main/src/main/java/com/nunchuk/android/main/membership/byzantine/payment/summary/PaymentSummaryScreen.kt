package com.nunchuk.android.main.membership.byzantine.payment.summary

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSnackBarHost
import com.nunchuk.android.compose.NcSnackbarVisuals
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.key.toRecurringPaymentType
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentViewModel
import com.nunchuk.android.model.FeeRate
import com.nunchuk.android.model.SpendingCurrencyUnit
import com.nunchuk.android.model.byzantine.DummyTransactionPayload
import com.nunchuk.android.model.payment.PaymentFrequency

@Composable
fun PaymentSummaryRoute(
    recurringPaymentViewModel: RecurringPaymentViewModel,
    openDummyTransactionScreen: (DummyTransactionPayload) -> Unit,
    openQRDetailScreen: (address: String) -> Unit,
) {
    val config by recurringPaymentViewModel.config.collectAsStateWithLifecycle()
    val state by recurringPaymentViewModel.state.collectAsStateWithLifecycle()
    val snackState = remember { SnackbarHostState() }

    LaunchedEffect(state.openDummyTransactionScreen) {
        state.openDummyTransactionScreen?.let {
            openDummyTransactionScreen(it)
            recurringPaymentViewModel.onOpenDummyTransactionScreenComplete()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackState.showSnackbar(
                NcSnackbarVisuals(
                    type = NcToastType.ERROR,
                    message = it,
                )
            )
            recurringPaymentViewModel.onErrorMessageShown()
        }
    }

    PaymentSummaryScreen(
        isCosign = config.isCosign,
        name = config.name,
        amount = config.amount,
        frequency = config.frequency ?: PaymentFrequency.DAILY,
        startDate = config.startDate,
        endDate = config.endDate,
        noEndDate = config.noEndDate,
        feeRate = config.feeRate,
        addresses = config.addresses,
        note = config.note,
        onSubmit = recurringPaymentViewModel::onSubmit,
        unit = config.unit,
        useAmount = config.useAmount,
        snackState = snackState,
        openQRDetailScreen = openQRDetailScreen,
        bsms = config.bsms,
    )
}

@Composable
fun PaymentSummaryScreen(
    onSubmit: () -> Unit = {},
    isCosign: Boolean? = null,
    name: String = "",
    amount: String = "",
    frequency: PaymentFrequency = PaymentFrequency.DAILY,
    startDate: Long = 0,
    endDate: Long = 0,
    noEndDate: Boolean = false,
    feeRate: FeeRate = FeeRate.PRIORITY,
    addresses: List<String> = emptyList(),
    note: String = "",
    unit: SpendingCurrencyUnit = SpendingCurrencyUnit.CURRENCY_UNIT,
    useAmount: Boolean = false,
    bsms: String? = null,
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    openQRDetailScreen: (address: String) -> Unit = {},
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
        }, snackbarHost = {
            NcSnackBarHost(snackState)
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
                note = note,
                currency = unit.toRecurringPaymentType(),
                useAmount = useAmount,
                openQRDetailScreen = openQRDetailScreen,
                bsms = bsms,
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
