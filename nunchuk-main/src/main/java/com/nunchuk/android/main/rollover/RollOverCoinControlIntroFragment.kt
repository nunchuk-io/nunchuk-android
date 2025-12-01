package com.nunchuk.android.main.rollover

import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.data.model.TxReceipt
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.main.R
import com.nunchuk.android.model.SigningPath
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.utils.parcelable
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RollOverCoinControlIntroFragment : Fragment() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val args: RollOverCoinControlIntroFragmentArgs by navArgs()

    private val rollOverWalletViewModel: RollOverWalletViewModel by activityViewModels()

    private val selectSigningPathLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data
            if (it.resultCode == RESULT_OK && data != null) {
                val signingPath = data.parcelable<SigningPath>(GlobalResultKey.SIGNING_PATH)
                val address = rollOverWalletViewModel.getAddress()
                openEstimateFeeScreen(address = address, signingPath = signingPath)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val sharedUiState by rollOverWalletViewModel.uiState.collectAsStateWithLifecycle()
                RollOverCoinControlIntroView(
                    onContinueClicked = {
                        val address = rollOverWalletViewModel.getAddress()
                        val isMiniscript = sharedUiState.oldWallet.miniscript.isNotEmpty()
                        handleRollOverSigningPathCheck(
                            rollOverWalletViewModel = rollOverWalletViewModel,
                            rollOverWalletParam = null,
                            address = address,
                            isMiniscript = isMiniscript,
                            selectSigningPathLauncher = selectSigningPathLauncher,
                            navigator = navigator,
                            openEstimateFeeScreen = { signingPath ->
                                openEstimateFeeScreen(address, signingPath)
                            }
                        )
                    },
                    onAddTagOrCollectionClicked = {
                        val isCollectionOrTagExist =
                            sharedUiState.coinCollections.isNotEmpty() || sharedUiState.coinTags.isNotEmpty()
                        if (isCollectionOrTagExist) {
                            findNavController().navigate(
                                RollOverCoinControlIntroFragmentDirections.actionRollOverCoinControlIntroFragmentToRollOverCoinControlFragment(
                                    oldWalletId = args.oldWalletId,
                                    newWalletId = args.newWalletId
                                )
                            )
                        } else {
                            findNavController().navigate(
                                RollOverCoinControlIntroFragmentDirections.actionRollOverCoinControlIntroFragmentToRollOverAddTagOrCollectionFragment(
                                    oldWalletId = args.oldWalletId,
                                    newWalletId = args.newWalletId
                                )
                            )
                        }
                    },
                    onKeepAllExistingCoinsClicked = {
                        findNavController().navigate(
                            RollOverCoinControlIntroFragmentDirections.actionRollOverCoinControlIntroFragmentToRollOverKeepAllExistingCoinsFragment(
                                oldWalletId = args.oldWalletId,
                                newWalletId = args.newWalletId
                            )
                        )
                    }
                )
            }
        }
    }

    private fun openEstimateFeeScreen(address: String, signingPath: SigningPath? = null) {
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
            confirmTxActionButtonText = getString(R.string.nc_confirm_withdraw_balance),
            signingPath = signingPath
        )
    }
}

@Composable
private fun RollOverCoinControlIntroView(
    onContinueClicked: () -> Unit = { },
    onAddTagOrCollectionClicked: () -> Unit = { },
    onKeepAllExistingCoinsClicked: () -> Unit = { },
) {
    RollOverCoinControlIntroContent(
        onContinueClicked = onContinueClicked,
        onAddTagOrCollectionClicked = onAddTagOrCollectionClicked,
        onKeepAllExistingCoinsClicked = onKeepAllExistingCoinsClicked
    )
}

@Composable
private fun RollOverCoinControlIntroContent(
    onContinueClicked: () -> Unit = { },
    onAddTagOrCollectionClicked: () -> Unit = { },
    onKeepAllExistingCoinsClicked: () -> Unit = { },
) {
    NunchukTheme {
        NcScaffold(
            modifier = Modifier.systemBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = "Coin control",
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
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth(),
                        onClick = {
                            onContinueClicked()
                        }) {
                        Text(text = "Continue consolidating all coins")
                    }

                    TextButton(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth(),
                        onClick = onAddTagOrCollectionClicked
                    ) {
                        Text(
                            text = stringResource(R.string.nc_keep_coin_tags_and_collections),
                            style = NunchukTheme.typography.title,
                        )
                    }

                    TextButton(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth(),
                        onClick = onKeepAllExistingCoinsClicked
                    ) {
                        Text(
                            text = stringResource(R.string.nc_keep_all_existing_coins),
                            style = NunchukTheme.typography.title,
                        )
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
                    text = "By default, all existing coins will be consolidated into a single coin in the new wallet. If you want to organize and segregate your coins for various purposes, consider using coin tags or collections before proceeding with the rollover, or keep all coins as they are.",
                    style = NunchukTheme.typography.body,
                )
            }
        }
    }
}

@Composable
@Preview
private fun RollOverCoinControlIntroScreenContentPreview() {
    RollOverCoinControlIntroContent()
}