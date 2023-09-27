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
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
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
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSpannedText
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.SpanIndicator
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.util.hideLoading
import com.nunchuk.android.core.util.orDefault
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.R
import com.nunchuk.android.model.Alert
import com.nunchuk.android.model.byzantine.AlertType
import com.nunchuk.android.model.byzantine.DummyTransactionPayload
import com.nunchuk.android.model.transaction.AlertPayload
import com.nunchuk.android.widget.NCToastMessage
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
                        setFragmentResult(
                            REQUEST_KEY, bundleOf(
                                EXTRA_DUMMY_TRANSACTION_ID to it.dummyTransactionId,
                                EXTRA_REQUIRE_KEY to it.requiredSignatures
                            )
                        )
                        goBack()
                    },
                    onCancel = {
                        val message = if (args.alert.type == AlertType.REQUEST_INHERITANCE_PLANNING) {
                            getString(R.string.nc_cancel_inheritance_planning_request)
                        } else {
                            getString(R.string.nc_cancel_health_check_request)
                        }
                        NCWarningDialog(requireActivity())
                            .showDialog(
                                title = getString(R.string.nc_confirmation),
                                message = message,
                                onYesClick = { viewModel.deleteDummyTransaction() },
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
                            if (args.alert.type == AlertType.REQUEST_INHERITANCE_PLANNING) {
                                NcToastManager.scheduleShowMessage(
                                    message = getString(R.string.nc_inheritance_request_denied),
                                    delay = 500L
                                )
                            }
                            hideLoading()
                            goBack()
                        }

                        is AlertActionIntroEvent.Loading -> showOrHideLoading(event.isLoading)
                    }
                }
        }
    }

    private fun goBack() {
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    companion object {
        const val REQUEST_KEY = "AlertActionIntroFragment"
        const val EXTRA_DUMMY_TRANSACTION_ID = "_a"
        const val EXTRA_REQUIRE_KEY = "_b"
    }
}

@Composable
private fun AlertActionIntroScreen(
    alert: Alert,
    viewModel: AlertActionIntroViewModel = viewModel(),
    onContinue: (DummyTransactionPayload) -> Unit,
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
    onContinue: (DummyTransactionPayload) -> Unit = {},
) {
    val cancelButton = when (alert.type) {
        AlertType.HEALTH_CHECK_PENDING -> stringResource(R.string.nc_cancel_health_check)
        AlertType.REQUEST_INHERITANCE_PLANNING -> stringResource(R.string.nc_deny)
        else -> stringResource(id = R.string.nc_cancel_request)
    }

    val body = when (alert.type) {
        AlertType.REQUEST_INHERITANCE_PLANNING -> stringResource(
            id = R.string.nc_inheritance_planning_request_desc,
            state.requester?.user?.name ?: "Someone",
            state.walletName
        )

        else -> alert.body
    }

    val continueButtonText = when (alert.type) {
        AlertType.REQUEST_INHERITANCE_PLANNING -> {
            stringResource(
                id = R.string.nc_approve_signature_pending,
                state.dummyTransaction?.pendingSignatures.orDefault(0)
            )
        }
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
                            onClick = { onContinue(state.dummyTransaction!!) },
                            enabled = state.dummyTransaction != null
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
                        Text(text = cancelButton)
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
                claimKey = false
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