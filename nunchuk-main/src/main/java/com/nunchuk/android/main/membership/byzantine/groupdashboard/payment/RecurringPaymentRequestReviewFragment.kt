package com.nunchuk.android.main.membership.byzantine.groupdashboard.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcConfirmationDialog
import com.nunchuk.android.core.util.MIN_FRACTION_DIGITS
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.formatDecimal
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.groupdashboard.action.AlertActionIntroFragment
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentProvider
import com.nunchuk.android.main.membership.byzantine.payment.summary.PaymentSummaryContent
import com.nunchuk.android.model.payment.RecurringPayment
import com.nunchuk.android.model.payment.RecurringPaymentType
import com.nunchuk.android.nav.NunchukNavigator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RecurringPaymentRequestReviewFragment : Fragment() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val args: RecurringPaymentRequestReviewFragmentArgs by navArgs()
    private val viewModel: RecurringPaymentRequestReviewViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                RecurringPaymentRequestReviewContent(
                    recurringPayment = args.recurringPayment,
                    pendingSignatures = args.pendingSignatures,
                    openDummyTransactionScreen = {
                        setFragmentResult(
                            AlertActionIntroFragment.REQUEST_KEY, bundleOf(
                                AlertActionIntroFragment.EXTRA_DUMMY_TRANSACTION_ID to args.dummyTransactionId,
                                AlertActionIntroFragment.EXTRA_REQUIRE_KEY to args.pendingSignatures
                            )
                        )
                        findNavController().popBackStack(R.id.groupDashboardFragment, false)
                    },
                    deleteRecurringPayment = viewModel::deleteDummyTransaction,
                    openQRDetailScreen = { address ->
                        findNavController().navigate(
                            RecurringPaymentRequestReviewFragmentDirections.actionRecurringPaymentRequestReviewFragmentToQrDetailFragment(
                                address
                            )
                        )
                    }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is RecurringPaymentRequestReviewEvent.DeleteDummyTransaction -> {
                    showSuccess(getString(R.string.nc_recurring_payment_denied))
                    findNavController().popBackStack(R.id.groupDashboardFragment, false)
                }

                is RecurringPaymentRequestReviewEvent.ShowError -> {
                    showError(event.message)
                }
            }
        }
    }
}


@Composable
private fun RecurringPaymentRequestReviewContent(
    recurringPayment: RecurringPayment,
    pendingSignatures: Int = 0,
    openDummyTransactionScreen: () -> Unit = {},
    deleteRecurringPayment: () -> Unit = {},
    openQRDetailScreen: (address: String) -> Unit,
) {
    var showDeletePaymentDialog by rememberSaveable { mutableStateOf(false) }
    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_recurring_payment),
                    textStyle = NunchukTheme.typography.titleLarge,
                )
            },
            bottomBar = {
                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        onClick = openDummyTransactionScreen,
                    ) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.nc_approve_signatures_pending,
                                pendingSignatures,
                                pendingSignatures,
                            )
                        )
                    }

                    TextButton(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        onClick = { showDeletePaymentDialog = true },
                    ) {
                        Text(text = stringResource(R.string.nc_deny))
                    }
                }
            }
        ) { innerPadding ->
            PaymentSummaryContent(
                modifier = Modifier.padding(innerPadding),
                isCosign = recurringPayment.allowCosigning,
                name = recurringPayment.name,
                amount = recurringPayment.amount.formatDecimal(
                    minFractionDigits = 0,
                    maxFractionDigits = MIN_FRACTION_DIGITS
                ),
                frequency = recurringPayment.frequency,
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
            )
        }

        if (showDeletePaymentDialog) {
            NcConfirmationDialog(
                message = stringResource(R.string.nc_delete_payment_desc),
                onPositiveClick = {
                    deleteRecurringPayment()
                    showDeletePaymentDialog = false
                },
            ) {
                showDeletePaymentDialog = false
            }
        }
    }
}

@Preview
@Composable
private fun RecurringPaymentRequestReviewScreenPreview(
    @PreviewParameter(RecurringPaymentProvider::class) recurringPayment: RecurringPayment,
) {
    RecurringPaymentRequestReviewContent(
        recurringPayment = recurringPayment,
        openQRDetailScreen = {})
}