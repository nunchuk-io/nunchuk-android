package com.nunchuk.android.transaction.components.details.fee

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcNumberInputField
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.greyDark
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.compose.wallet.AddressWithQrView
import com.nunchuk.android.compose.whisper
import com.nunchuk.android.core.util.CurrencyFormatter
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.send.fee.toFeeRate
import com.nunchuk.android.transaction.components.send.fee.toFeeRateInBtc
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.roundToInt

@AndroidEntryPoint
class RbfCancelTransactionFragment : Fragment() {
    private val viewModel: RbfCancelTransactionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                RbfCancelTransactionScreen(
                    viewModel = viewModel,
                    onCustomizeDestinationClick = { newFeeRate ->
                        findNavController().navigate(
                            RbfCancelTransactionFragmentDirections.actionRbfCancelTransactionFragmentToRbfCustomizeDestinationFragment(
                                newFeeRate,
                                antiFeeSniping = viewModel.state.value.antiFeeSniping
                            )
                        )
                    },
                    onContinueClick = { newFeeRate ->
                        viewModel.draftCancelTransaction(newFeeRate, viewModel.state.value.address)
                    }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) {
            when (it) {
                is ReplaceFeeEvent.ShowError -> showError(it.e?.message.orUnknownError())
                is ReplaceFeeEvent.DraftTransactionSuccess -> {
                    findNavController().navigate(
                        RbfCancelTransactionFragmentDirections.actionRbfCancelTransactionFragmentToConfirmReplaceTransactionFragment(
                            newFee = it.newFee,
                            address = viewModel.state.value.address,
                            antiFeeSniping = viewModel.state.value.antiFeeSniping
                        )
                    )
                }

                is ReplaceFeeEvent.Loading -> showOrHideLoading(it.isLoading)
                else -> Unit
            }
        }
    }
}

@Composable
private fun RbfCancelTransactionScreen(
    viewModel: RbfCancelTransactionViewModel = viewModel(),
    onContinueClick: (Int) -> Unit = {},
    onCustomizeDestinationClick: (Int) -> Unit = {},
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    RbfCancelTransactionContent(
        uiState = uiState,
        onContinueClick = onContinueClick,
        onCustomizeDestinationClick = onCustomizeDestinationClick,
        onAntiFeeSnipingChange = viewModel::onAntiFeeSnipingChange
    )
}

@Composable
private fun RbfCancelTransactionContent(
    uiState: RbfCancelTransactionUiState = RbfCancelTransactionUiState(),
    onContinueClick: (Int) -> Unit = {},
    onCustomizeDestinationClick: (Int) -> Unit = {},
    onAntiFeeSnipingChange: () -> Unit = {},
) {
    var newFeeRate by rememberSaveable {
        mutableStateOf("")
    }
    var showWarning by rememberSaveable {
        mutableStateOf(false)
    }
    NunchukTheme {
        Scaffold(topBar = {
            NcTopAppBar(title = "")
        }, bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    enabled = newFeeRate.isNotEmpty(),
                    onClick = {
                        val newFee = newFeeRate.toDouble().times(1000).roundToInt()
                        if (newFee > uiState.previousFeeRate) {
                            onContinueClick(newFee)
                        } else {
                            showWarning = true
                        }
                    },
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }

                NcOutlineButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    onClick = { onCustomizeDestinationClick(newFeeRate.toDouble().times(1000).roundToInt()) },
                    enabled = newFeeRate.isNotEmpty()
                ) {
                    Text(text = stringResource(R.string.nc_customize_destination))
                }
            }
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.nc_transaction_cancel_transaction),
                    style = NunchukTheme.typography.heading
                )

                Text(
                    text = stringResource(R.string.nc_cancel_pending_confirm_tx_desc),
                    style = NunchukTheme.typography.body,
                    modifier = Modifier.padding(top = 16.dp)
                )

                Text(
                    text = stringResource(R.string.nc_wallet_address),
                    style = NunchukTheme.typography.title,
                    modifier = Modifier.padding(top = 24.dp)
                )

                AddressWithQrView(address = uiState.address)

                Text(
                    text = stringResource(R.string.nc_fee_rate),
                    style = NunchukTheme.typography.title,
                    modifier = Modifier.padding(top = 24.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.greyLight,
                            shape = NunchukTheme.shape.medium
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.nc_old_fee_rate),
                            style = NunchukTheme.typography.body
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = uiState.previousFeeRate.toFeeRate(),
                                style = NunchukTheme.typography.title
                            )

                            Text(
                                text = uiState.previousFeeRate.toFeeRateInBtc(),
                                style = NunchukTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                            .height(1.dp)
                            .background(color = MaterialTheme.colorScheme.whisper),
                    )

                    Text(
                        text = stringResource(R.string.nc_new_fee_rate),
                        style = NunchukTheme.typography.body
                    )

                    Text(
                        text = stringResource(R.string.nc_new_fee_rate_desc),
                        style = NunchukTheme.typography.bodySmall
                    )

                    Row(
                        Modifier.padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NcNumberInputField(
                            modifier = Modifier.weight(1f),
                            title = "",
                            value = newFeeRate,
                            onValueChange = {
                                showWarning = false
                                val format = CurrencyFormatter.format(it, 3)
                                newFeeRate = format
                            },
                            error = stringResource(R.string.nc_new_fee_rate_invalid).takeIf { showWarning },
                        )

                        Text(
                            text = stringResource(R.string.nc_transaction_fee_rate_unit),
                            style = NunchukTheme.typography.titleSmall,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }

                    Text(
                        text = stringResource(id = R.string.nc_transaction_processing_speed),
                        style = NunchukTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        FeeRateView(
                            modifier = Modifier.weight(1f),
                            title = stringResource(id = R.string.nc_transaction_priority_rate),
                            value = uiState.fee.priorityRate
                        )
                        FeeRateView(
                            modifier = Modifier.weight(1f),
                            title = stringResource(id = R.string.nc_transaction_standard_rate),
                            value = uiState.fee.standardRate
                        )
                        FeeRateView(
                            modifier = Modifier.weight(1f),
                            title = stringResource(id = R.string.nc_transaction_economical_rate),
                            value = uiState.fee.economicRate
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Anti-fee sniping",
                                style = NunchukTheme.typography.body,
                            )

                            Text(
                                modifier = Modifier.padding(top = 4.dp),
                                text = "Adds the latest block height to the PSBT to prevent fee sniping attacks.",
                                style = NunchukTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.textSecondary
                            )
                        }

                        Checkbox(checked = uiState.antiFeeSniping, onCheckedChange = {
                            onAntiFeeSnipingChange()
                        }, modifier = Modifier.padding(top = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FeeRateView(modifier: Modifier, title: String, value: Int) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = NunchukTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.greyDark)
        )

        Text(
            text = value.toFeeRate(),
            style = NunchukTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary)
        )

    }
}

@Preview
@Composable
private fun RbfCancelTransactionScreenPreview() {
    RbfCancelTransactionContent(
        uiState = RbfCancelTransactionUiState(
            address = "bc1qft5swsj3nnm48qcpwvsqmht899gf6zk28tvvq6pdkjekaq3rltaqkztttq"
        )
    )
}