package com.nunchuk.android.main.rollover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.HighlightMessageType
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.backgroundLightGray
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.compose.whisper
import com.nunchuk.android.core.data.model.RollOverWalletParam
import com.nunchuk.android.core.data.model.TxReceipt
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.main.R
import com.nunchuk.android.nav.NunchukNavigator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RollOverKeepAllExistingCoinsFragment : Fragment() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: RollOverKeepAllExistingCoinsViewModel by viewModels()
    private val rollOverWalletViewModel: RollOverWalletViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                RollOverKeepAllExistingCoinsView(
                    uiState = uiState,
                    onContinueClicked = {
                        val rollOverWalletParam = RollOverWalletParam(
                            newWalletId = rollOverWalletViewModel.getNewWalletId(),
                            tags = emptyList(),
                            collections = emptyList(),
                            source = rollOverWalletViewModel.getSource()
                        )
                        val address = rollOverWalletViewModel.getAddress()
                        navigator.openEstimatedFeeScreen(
                            activityContext = requireActivity(),
                            walletId = rollOverWalletViewModel.getOldWalletId(),
                            availableAmount = rollOverWalletViewModel.getOldWallet().balance.pureBTC(),
                            txReceipts = listOf(
                                TxReceipt(
                                    address = address,
                                    amount = rollOverWalletViewModel.getOldWallet().balance.pureBTC()
                                )
                            ),
                            privateNote = "",
                            subtractFeeFromAmount = true,
                            title = getString(R.string.nc_transaction_new),
                            rollOverWalletParam = rollOverWalletParam,
                            confirmTxActionButtonText = getString(R.string.nc_confirm_withdraw_balance)
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
                is RollOverKeepAllExistingCoinsEvent.Error -> showError(event.message)
            }
        }
    }
}

@Composable
private fun RollOverKeepAllExistingCoinsView(
    uiState: RollOverKeepAllExistingCoinsUiState = RollOverKeepAllExistingCoinsUiState(),
    onContinueClicked: () -> Unit = { },
) {
    RollOverKeepAllExistingCoinsContent(
        uiState = uiState,
        onContinueClicked = onContinueClicked
    )
}

@Composable
private fun RollOverKeepAllExistingCoinsContent(
    uiState: RollOverKeepAllExistingCoinsUiState = RollOverKeepAllExistingCoinsUiState(),
    onContinueClicked: () -> Unit = { },
) {
    NunchukTheme {
        if (uiState.isLoading) {
            NcLoadingDialog()
        }
        NcScaffold(
            modifier = Modifier.systemBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_coin_control),
                    textStyle = NunchukTheme.typography.title,
                    actions = {
                        Spacer(modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize))
                    })
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    NcHintMessage(
                        modifier = Modifier.fillMaxWidth(),
                        messages = listOf(
                            ClickAbleText(
                                content = stringResource(R.string.nc_coins_dust_limit_warning)
                            )
                        ),
                        type = HighlightMessageType.WARNING
                    )
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth(),
                        onClick = onContinueClicked
                    ) {
                        Text(text = stringResource(R.string.nc_continue))
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_circle_coin_control),
                        contentDescription = ""
                    )
                }

                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(R.string.nc_keep_all_existing_coins),
                    style = NunchukTheme.typography.titleLarge,
                )

                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(R.string.nc_keep_all_existing_coins_desc),
                    style = NunchukTheme.typography.body,
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 24.dp),
                    color = MaterialTheme.colorScheme.whisper,
                    thickness = 1.dp
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.backgroundLightGray,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.nc_required_transactions),
                            style = NunchukTheme.typography.body,
                        )
                        Text(
                            text = "${uiState.numOfTxs}",
                            style = NunchukTheme.typography.body,
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.nc_total_estimated_fees),
                            style = NunchukTheme.typography.body,
                        )
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = uiState.feeAmount.getBTCAmount(),
                                style = NunchukTheme.typography.title,
                            )
                            Text(
                                modifier = Modifier.padding(top = 4.dp),
                                text = uiState.feeAmount.getCurrencyAmount(),
                                style = NunchukTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Preview
private fun RollOverKeepAllExistingCoinsScreenContentPreview() {
    RollOverKeepAllExistingCoinsContent()
}

