package com.nunchuk.android.main.membership.byzantine.groupdashboard.action

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSpannedText
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.SpanIndicator
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.util.hideLoading
import com.nunchuk.android.core.util.orDefault
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.groupdashboard.GroupDashboardFragmentDirections
import com.nunchuk.android.model.Alert
import com.nunchuk.android.model.byzantine.AlertType
import com.nunchuk.android.model.byzantine.DummyTransactionPayload
import com.nunchuk.android.model.transaction.AlertPayload
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AlertActionIntroFragment : Fragment() {
    private val viewModel: AlertActionIntroViewModel by viewModels()
    private val args: AlertActionIntroFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AlertActionIntroScreen(
                    alert = args.alert, viewModel = viewModel,
                    onContinue = {
                        if (args.alert.type == AlertType.REQUEST_INHERITANCE_PLANNING) {
                           viewModel.approveInheritanceRequestPlanning()
                        } else if (args.alert.type == AlertType.HEALTH_CHECK_REMINDER) {
                            openHealthCheckScreen()
                        } else if (it != null) {
                            setFragmentResult(
                                REQUEST_KEY, bundleOf(
                                    EXTRA_DUMMY_TRANSACTION_ID to it.dummyTransactionId,
                                    EXTRA_REQUIRE_KEY to it.requiredSignatures
                                )
                            )
                            goBack()
                        }
                    },
                    onCancel = {
                        if (args.alert.type == AlertType.HEALTH_CHECK_REMINDER) {
                            viewModel.skipHealthReminder()
                            return@AlertActionIntroScreen
                        }
                        val message = when (args.alert.type) {
                            AlertType.REQUEST_INHERITANCE_PLANNING -> {
                                getString(R.string.nc_cancel_inheritance_planning_request)
                            }
                            AlertType.KEY_RECOVERY_REQUEST, AlertType.CHANGE_EMAIL_REQUEST -> {
                                getString(R.string.nc_cancel_this_change)
                            }
                            AlertType.RECURRING_PAYMENT_CANCELATION_PENDING -> {
                               getString(R.string.nc_cancel_recurring_payment_desc)
                            }
                            else -> {
                                getString(R.string.nc_cancel_health_check_request)
                            }
                        }
                        NCWarningDialog(requireActivity())
                            .showDialog(
                                title = getString(R.string.nc_confirmation),
                                message = message,
                                onYesClick = {
                                    if (args.alert.type == AlertType.REQUEST_INHERITANCE_PLANNING) {
                                        viewModel.denyInheritanceRequestPlanning()
                                    } else {
                                        viewModel.deleteDummyTransaction()
                                    }
                                }
                            )
                    },
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        is AlertActionIntroEvent.DeleteDummyTransactionSuccess -> {
                            when (args.alert.type) {
                                AlertType.HEALTH_CHECK_REQUEST, AlertType.HEALTH_CHECK_PENDING -> {
                                    showSuccess(
                                        message = getString(R.string.nc_health_check_has_been_canceled),
                                    )
                                }
                                AlertType.RECURRING_PAYMENT_CANCELATION_PENDING -> {
                                    showSuccess(
                                        message = getString(R.string.nc_pending_cancellation_has_been_canceled),
                                    )
                                }
                                AlertType.CHANGE_EMAIL_REQUEST -> {
                                    NcToastManager.scheduleShowMessage(
                                        message = getString(R.string.nc_change_email_request_has_been_canceled),
                                    )
                                }
                                else -> Unit
                            }
                            hideLoading()
                            goBack()
                        }

                        is AlertActionIntroEvent.Loading -> showOrHideLoading(event.isLoading)
                        AlertActionIntroEvent.ApproveInheritanceRequestPlanningSuccess -> {
                            setFragmentResult(
                                REQUEST_KEY,
                                bundleOf(EXTRA_APPROVE_INHERITANCE_REQUEST to true)
                            )
                            goBack()
                        }
                        AlertActionIntroEvent.DenyInheritanceRequestPlanningSuccess -> {
                            setFragmentResult(
                                REQUEST_KEY,
                                bundleOf(EXTRA_APPROVE_INHERITANCE_REQUEST to false)
                            )
                            goBack()
                        }

                        is AlertActionIntroEvent.Error -> showError(message = event.message)
                        AlertActionIntroEvent.SkipHealthReminderSuccess -> {
                            goBack()
                        }
                    }
                }
        }
    }

    private fun openHealthCheckScreen() {
        val walletId = args.walletId
        if (walletId.isNotEmpty()) {
            findNavController().navigate(
                AlertActionIntroFragmentDirections.actionAlertActionIntroFragmentToHealthCheckFragment(
                    groupId = args.groupId,
                    walletId = args.walletId
                ),
                NavOptions.Builder().setPopUpTo(R.id.groupDashboardFragment, false).build()
            )
        }
    }

    private fun goBack() {
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    companion object {
        const val REQUEST_KEY = "AlertActionIntroFragment"
        const val EXTRA_DUMMY_TRANSACTION_ID = "_a"
        const val EXTRA_REQUIRE_KEY = "_b"
        const val EXTRA_APPROVE_INHERITANCE_REQUEST = "_c"
    }
}

@Composable
private fun AlertActionIntroScreen(
    alert: Alert,
    viewModel: AlertActionIntroViewModel = viewModel(),
    onContinue: (DummyTransactionPayload?) -> Unit,
    onCancel: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AlertActionIntroContent(
        state = state,
        alert = alert,
        onCancelRequest = onCancel,
        onContinue = onContinue
    )
}

@Composable
private fun AlertActionIntroContent(
    state: AlertActionIntroUiState = AlertActionIntroUiState(),
    alert: Alert,
    onCancelRequest: () -> Unit = {},
    onContinue: (DummyTransactionPayload?) -> Unit = {},
) {
    val cancelButton = when (alert.type) {
        AlertType.HEALTH_CHECK_PENDING -> stringResource(R.string.nc_cancel_health_check)
        AlertType.HEALTH_CHECK_REMINDER -> stringResource(R.string.nc_skip_health_check)
        AlertType.REQUEST_INHERITANCE_PLANNING -> stringResource(R.string.nc_deny)
        AlertType.CHANGE_EMAIL_REQUEST -> stringResource(R.string.nc_cancel_change)
        AlertType.KEY_RECOVERY_REQUEST, AlertType.RECURRING_PAYMENT_CANCELATION_PENDING -> stringResource(R.string.nc_cancel)
        else -> stringResource(id = R.string.nc_cancel_request)
    }

    val body = when (alert.type) {
        AlertType.REQUEST_INHERITANCE_PLANNING -> stringResource(
            id = R.string.nc_inheritance_planning_request_desc,
            state.requester?.user?.name ?: "Someone",
            state.walletName
        )
        AlertType.RECURRING_PAYMENT_CANCELATION_PENDING -> stringResource(
            id = R.string.nc_recurring_payment_cancelation_pending_desc,
            alert.payload.paymentName.orEmpty()
        )
        AlertType.UPDATE_SECURITY_QUESTIONS -> stringResource(
            id = R.string.nc_security_questions_answer_will_be_updated
        )
        AlertType.CHANGE_EMAIL_REQUEST -> stringResource(
            id = R.string.nc_change_email_request_desc, state.changeEmail?.oldEmail.orEmpty(), state.changeEmail?.newEmail.orEmpty()
        )
        AlertType.HEALTH_CHECK_REMINDER -> stringResource(
            id = R.string.nc_it_time_check_the_heath_of, state.signer?.name.orEmpty()
        )
        else -> alert.body
    }

    val continueButtonText = when (alert.type) {
        AlertType.REQUEST_INHERITANCE_PLANNING -> stringResource(id = R.string.nc_approve)
        else -> stringResource(id = R.string.nc_text_continue)
    }
    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding(),
            topBar = {
                NcTopAppBar(title = "", isBack = false)
            },
            bottomBar = {
                Column {
                    if (alert.type != AlertType.HEALTH_CHECK_REQUEST) {
                        NcPrimaryDarkButton(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            onClick = { onContinue(state.dummyTransaction) },
                            enabled = state.dummyTransaction != null || alert.type == AlertType.REQUEST_INHERITANCE_PLANNING || alert.type == AlertType.HEALTH_CHECK_REMINDER
                        ) {
                            Text(text = continueButtonText)
                        }
                    }

                    TextButton(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        onClick = onCancelRequest
                    ) {
                        Text(text = cancelButton, style = NunchukTheme.typography.title)
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = alert.title,
                    style = NunchukTheme.typography.heading
                )
                NcSpannedText(
                    modifier = Modifier.padding(top = 16.dp),
                    text = body,
                    baseStyle = NunchukTheme.typography.body,
                    styles = mapOf(SpanIndicator('A') to SpanStyle(fontWeight = FontWeight.Bold), SpanIndicator('B') to SpanStyle(fontWeight = FontWeight.Bold), SpanIndicator('C') to SpanStyle(fontWeight = FontWeight.Bold))
                )
            }
        }
    }
}

@Preview
@Composable
private fun AlertActionIntroScreenPreview() {
    AlertActionIntroContent(
        alert = Alert(
            viewable = true,
            payload = AlertPayload(
                dummyTransactionId = "dummyTransactionId",
                pendingKeysCount = 1,
                masterName = "masterName",
                xfps = listOf("xfps"),
                claimKey = false,
                keyXfp = "keyXfp",
                requestId = "123",
                membershipId = "123",
                transactionId = "123",
                xfp = "xfp"
            ),
            body = "There is a health check request for [key name].",
            createdTimeMillis = 0,
            id = "id",
            status = "status",
            title = "[name of key]: health check requested",
            type = AlertType.DRAFT_WALLET_KEY_ADDED
        )
    )
}