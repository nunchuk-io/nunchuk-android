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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSpannedText
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.SpanIndicator
import com.nunchuk.android.main.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecurringPaymentRequestFragment : Fragment() {
    private val args: RecurringPaymentRequestFragmentArgs by navArgs()
    private val viewModel: RecurringPaymentRequestViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                RecurringPaymentRequestScreen(viewModel) {
                    val state = viewModel.state.value
                    state.recurringPayment?.let {
                        findNavController().navigate(
                            RecurringPaymentRequestFragmentDirections
                                .actionRecurringPaymentRequestFragmentToRecurringPaymentRequestReviewFragment(
                                    recurringPayment = it,
                                    pendingSignatures = state.pendingSignatures,
                                    dummyTransactionId = args.dummyTransactionId,
                                    walletId = args.walletId,
                                    groupId = args.groupId,
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringPaymentRequestScreen(
    viewModel: RecurringPaymentRequestViewModel = viewModel(),
    openRequestSummaryScreen : () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RecurringPaymentRequestContent(
        requestName = state.requester?.emailOrUsername.orEmpty(),
        paymentName = state.recurringPayment?.name.orEmpty(),
        openRequestSummaryScreen = openRequestSummaryScreen,
    )
}

@Composable
private fun RecurringPaymentRequestContent(
    requestName: String = "",
    paymentName: String = "",
    openRequestSummaryScreen : () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding(),
            topBar = {
                NcTopAppBar(title = "", isBack = false)
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    onClick = openRequestSummaryScreen,
                    enabled = paymentName.isNotEmpty()
                ) {
                    Text(text = stringResource(R.string.nc_review_recurring_payment))
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.nc_recurring_payment_request),
                    style = NunchukTheme.typography.heading
                )

                NcSpannedText(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(
                        id = R.string.nc_recurring_payment_request_desc,
                        requestName,
                        paymentName
                    ),
                    baseStyle = NunchukTheme.typography.body,
                    styles = mapOf(SpanIndicator('A') to SpanStyle(fontWeight = FontWeight.Bold))
                )
            }
        }
    }
}

@Preview
@Composable
private fun RecurringPaymentRequestScreenPreview() {
    RecurringPaymentRequestContent(
        requestName = "John Doe",
        paymentName = "Payment Name",
    )
}