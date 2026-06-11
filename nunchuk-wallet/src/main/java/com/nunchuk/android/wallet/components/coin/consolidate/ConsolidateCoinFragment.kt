package com.nunchuk.android.wallet.components.coin.consolidate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcInfoDialog
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.compose.wallet.AddressWithQrView
import com.nunchuk.android.core.data.model.TxReceipt
import com.nunchuk.android.core.util.MAX_NOTE_LENGTH
import com.nunchuk.android.core.util.fromSATtoBTC
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.utils.MaxLengthTransformation
import com.nunchuk.android.wallet.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ConsolidateCoinFragment : Fragment() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val args: ConsolidateCoinFragmentArgs by navArgs()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val viewModel = hiltViewModel<ConsolidateCoinViewModel>()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                ConsolidateCoinScreen(
                    state = state,
                    onCreateTransaction = { note ->
                        val availableAmount =
                            args.selectedCoins.sumOf { it.amount.value }.toDouble().fromSATtoBTC()
                        navigator.openTransactionConfirmScreen(
                            activityContext = requireActivity(),
                            walletId = args.walletId,
                            availableAmount = availableAmount,
                            txReceipts = listOf(
                                TxReceipt(
                                    address = state.address,
                                    amount = availableAmount,
                                )
                            ),
                            privateNote = note,
                            subtractFeeFromAmount = true,
                            manualFeeRate = state.manualFeeRate,
                            inputs = args.selectedCoins.toList(),
                            antiFeeSniping = false
                        )
                    },
                    onCustomizeTransaction = { note ->
                        val availableAmount =
                            args.selectedCoins.sumOf { it.amount.value }.toDouble().fromSATtoBTC()

                        navigator.openEstimatedFeeScreen(
                            activityContext = requireActivity(),
                            walletId = args.walletId,
                            availableAmount = availableAmount,
                            txReceipts = listOf(
                                TxReceipt(
                                    address = state.address,
                                    amount = availableAmount,
                                )
                            ),
                            privateNote = note,
                            subtractFeeFromAmount = true,
                            inputs = args.selectedCoins.toList(),
                            isConsolidateFlow = true
                        )
                    },
                    handleGenerateAddressError = viewModel::handleGenerateAddressError
                )
            }
        }
    }
}

@Composable
fun ConsolidateCoinScreen(
    state: ConsolidateCoinUiState,
    onCreateTransaction: (String) -> Unit = {},
    onCustomizeTransaction: (String) -> Unit = {},
    handleGenerateAddressError: () -> Unit = {},
) {
    var note by rememberSaveable { mutableStateOf("") }
    NunchukTheme {
        if (state.showGenerateAddressError) {
            NcInfoDialog(
                message = stringResource(R.string.nc_consolidate_generate_address_error_msg),
                onPositiveClick = handleGenerateAddressError,
                onDismiss = handleGenerateAddressError
            )
        }
        if (state.isLoading) {
            NcLoadingDialog()
        }
        Scaffold(
            modifier = Modifier.systemBarsPadding(),
            topBar = {
                NcTopAppBar(title = "")
            },
            bottomBar = {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth(),
                        onClick = { onCreateTransaction(note) }
                    ) {
                        Text(text = stringResource(R.string.nc_create_transaction))
                    }

                    NcOutlineButton(
                        modifier = Modifier
                            .fillMaxWidth(),
                        onClick = { onCustomizeTransaction(note) },
                    ) {
                        Text(text = stringResource(R.string.nc_customize_transaction))
                    }
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.nc_consolidate_coins),
                    style = NunchukTheme.typography.heading,
                )

                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(R.string.nc_consolidate_coin_desc),
                    style = NunchukTheme.typography.body,
                )

                Text(
                    modifier = Modifier.padding(top = 24.dp),
                    text = "Wallet address",
                    style = NunchukTheme.typography.title,
                )

                AddressWithQrView(state.address)

                NcTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    title = stringResource(id = R.string.nc_transaction_note),
                    value = note,
                    minLines = 3,
                    maxLength = MAX_NOTE_LENGTH,
                    enableMaxLength = true,
                    visualTransformation = MaxLengthTransformation(maxLength = MAX_NOTE_LENGTH),
                    onValueChange = {
                        if (it.length <= MAX_NOTE_LENGTH) {
                            note = it
                        }
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewConsolidateCoinScreen() {
    ConsolidateCoinScreen(
        state = ConsolidateCoinUiState(
            address = "1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2",
        )
    )
}

