package com.nunchuk.android.main.membership.byzantine.payment.detail

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcConfirmationDialog
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentProvider
import com.nunchuk.android.main.membership.byzantine.payment.summary.PaymentSummaryContent
import com.nunchuk.android.model.byzantine.DummyTransactionPayload
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.model.payment.RecurringPayment
import com.nunchuk.android.model.payment.RecurringPaymentType
import com.nunchuk.android.utils.formatAmount

@Composable
fun RecurringPaymentDetailRoute(
    viewModel: RecurringPaymentDetailViewModel = hiltViewModel(),
    onOpenDummyTransaction: (DummyTransactionPayload) -> Unit = {},
    openQRDetailScreen: (address: String) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var groupType by remember {
        mutableStateOf<GroupWalletType?>(null)
    }
    LaunchedEffect(Unit) {
        if (state.recurringPayment?.allowCosigning == true) {
            groupType = viewModel.getGroupConfig()
        }
    }

    state.openDummyTransactionPayload?.let {
        onOpenDummyTransaction(it)
        viewModel.onOpenDummyTransactionScreenComplete()
    }

    RecurringPaymentDetailScreen(
        recurringPayment = state.recurringPayment,
        groupType = groupType,
        onCancelPayment = viewModel::onCancelPayment,
        isLoading = state.isLoading,
        openQRDetailScreen = openQRDetailScreen
    )
}

@Composable
fun RecurringPaymentDetailScreen(
    onCancelPayment: () -> Unit = {},
    recurringPayment: RecurringPayment? = null,
    groupType: GroupWalletType? = null,
    isLoading: Boolean = false,
    openQRDetailScreen: (address: String) -> Unit = {},
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    NunchukTheme {
        Scaffold(topBar = {
            NcTopAppBar(
                title = stringResource(R.string.nc_recurring_payment),
                textStyle = NunchukTheme.typography.titleLarge,
            )
        }, bottomBar = {
            TextButton(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                onClick = { showDeleteDialog = true },
                enabled = recurringPayment != null,
            ) {
                Text(text = stringResource(R.string.nc_cancel_this_payment))
            }
        }) { innerPadding ->
            recurringPayment?.let {
                PaymentSummaryContent(
                    modifier = Modifier.padding(innerPadding),
                    isCosign = recurringPayment.allowCosigning,
                    name = recurringPayment.name,
                    amount = recurringPayment.formatAmount,
                    frequency = recurringPayment.frequency,
                    destinationType = recurringPayment.destinationType,
                    calculationMethod = recurringPayment.calculationMethod,
                    startDate = recurringPayment.startDate,
                    noEndDate = recurringPayment.endDate == 0L,
                    endDate = recurringPayment.endDate,
                    feeRate = recurringPayment.feeRate,
                    addresses = recurringPayment.addresses,
                    note = recurringPayment.note,
                    currency = recurringPayment.currency,
                    useAmount = recurringPayment.paymentType == RecurringPaymentType.FIXED_AMOUNT,
                    openQRDetailScreen = openQRDetailScreen,
                    bsms = recurringPayment.bsms,
                    groupWalletType = groupType,
                )
            }
        }

        if (showDeleteDialog) {
            NcConfirmationDialog(
                message = stringResource(R.string.nc_delete_recurring_payment_message),
                onPositiveClick = {
                    onCancelPayment()
                    showDeleteDialog = false
                },
                onDismiss = { showDeleteDialog = false },
            )
        }
        if (isLoading) {
            NcLoadingDialog()
        }
    }
}

@Preview
@Composable
fun RecurringPaymentDetailScreenPreview(
    @PreviewParameter(RecurringPaymentProvider::class) recurringPayment: RecurringPayment,
) {
    RecurringPaymentDetailScreen(
        recurringPayment = recurringPayment,
    )
}