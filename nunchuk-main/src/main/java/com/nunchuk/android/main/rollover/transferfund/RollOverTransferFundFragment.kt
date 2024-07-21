package com.nunchuk.android.main.rollover.transferfund

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.R
import com.nunchuk.android.main.rollover.RollOverWalletViewModel
import com.nunchuk.android.main.rollover.coincontrol.RollOverCoinControlEvent
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nav.NunchukNavigator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RollOverTransferFundFragment : Fragment() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val args: RollOverTransferFundFragmentArgs by navArgs()

    private val viewModel: RollOverTransferFundViewModel by viewModels()
    private val rollOverWalletViewModel: RollOverWalletViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                RollOverTransferFundView(
                    viewModel = viewModel,
                    onContinueClicked = {
                        if (viewModel.isHasTagOrCollection()) {
                            findNavController().navigate(
                                RollOverTransferFundFragmentDirections.actionRollOverTransferFundFragmentToRollOverCoinControlFragment(
                                    oldWalletId = args.oldWalletId,
                                    newWalletId = args.newWalletId
                                )
                            )
                        } else {
                            findNavController().navigate(
                                RollOverTransferFundFragmentDirections.actionRollOverTransferFundFragmentToRollOverCoinControlIntroFragment(
                                    oldWalletId = args.oldWalletId,
                                    newWalletId = args.newWalletId
                                )
                            )
                        }
                    },
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        flowObserver(rollOverWalletViewModel.uiState) { state ->
            viewModel.updateWallets(state.oldWallet, state.newWallet)
        }
    }
}

@Composable
private fun RollOverTransferFundView(
    viewModel: RollOverTransferFundViewModel = hiltViewModel(),
    onContinueClicked: () -> Unit = { },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RollOverTransferFundContent(
        uiState = uiState,
        onContinueClicked = onContinueClicked
    )
}

@Composable
private fun RollOverTransferFundContent(
    uiState: RollOverTransferFundUiState = RollOverTransferFundUiState(),
    onContinueClicked: () -> Unit = { },
) {
    NunchukTheme {
        NcScaffold(
            modifier = Modifier.systemBarsPadding(),
            topBar = {
                NcTopAppBar(title = "")
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    onClick = onContinueClicked) {
                    Text(text = stringResource(R.string.nc_text_continue))
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(R.string.nc_transfer_funds),
                    style = NunchukTheme.typography.heading,
                )

                Text(
                    text = stringResource(
                        R.string.nc_transfer_funds_desc,
                        uiState.newWallet.name
                    ),
                    style = NunchukTheme.typography.body,
                    modifier = Modifier.padding(top = 16.dp)
                )

                Row(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.greyLight,
                            shape = NunchukTheme.shape.medium
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.nc_existing_balance),
                        style = NunchukTheme.typography.body,
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = uiState.oldWallet.getBTCAmount(),
                            style = NunchukTheme.typography.title,
                        )

                        Text(
                            text = uiState.oldWallet.getCurrencyAmount(),
                            style = NunchukTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Preview
private fun RollOverTransferFundScreenContentPreview() {
    RollOverTransferFundContent(
        uiState = RollOverTransferFundUiState()
    )
}