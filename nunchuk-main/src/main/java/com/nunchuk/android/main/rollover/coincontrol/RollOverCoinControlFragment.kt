package com.nunchuk.android.main.rollover.coincontrol

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.CollectionItem
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.TagItem
import com.nunchuk.android.compose.whisper
import com.nunchuk.android.core.coin.CollectionFlow
import com.nunchuk.android.core.coin.TagFlow
import com.nunchuk.android.core.data.model.RollOverWalletParam
import com.nunchuk.android.core.data.model.TxReceipt
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.R
import com.nunchuk.android.main.rollover.RollOverWalletViewModel
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nav.NunchukNavigator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RollOverCoinControlFragment : Fragment() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: RollOverCoinControlViewModel by viewModels()
    private val rollOverWalletViewModel: RollOverWalletViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                RollOverCoinControlView(
                    viewModel = viewModel,
                    onContinueClicked = {
                        val rollOverWalletParam = if (viewModel.isSelectTagOrCollection()) {
                            RollOverWalletParam(
                                newWalletId = rollOverWalletViewModel.getNewWalletId(),
                                tags = viewModel.getSelectedCoinTags(),
                                collections = viewModel.getSelectedCoinCollections(),
                                source = rollOverWalletViewModel.getSource()
                            )
                        } else {
                            null
                        }
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
                    },
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.init(
            rollOverWalletViewModel.getOldWalletId(),
            rollOverWalletViewModel.getNewWalletId()
        )

        flowObserver(viewModel.event) { event ->
            when (event) {
                is RollOverCoinControlEvent.Loading -> showOrHideLoading(event.isLoading)
                is RollOverCoinControlEvent.Error -> showError(event.message)
            }
        }

        flowObserver(rollOverWalletViewModel.uiState) { state ->
            viewModel.updateTags(state.coins, state.coinTags)
            viewModel.updateCollections(state.coins, state.coinCollections)
        }
    }
}

@Composable
private fun RollOverCoinControlView(
    viewModel: RollOverCoinControlViewModel = hiltViewModel(),
    onContinueClicked: () -> Unit = { },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RollOverCoinControlContent(
        uiState = uiState,
        onContinueClicked = onContinueClicked,
        onTagCheckedChange = viewModel::setSelectTag,
        onCollectionCheckedChange = viewModel::setSelectCollection,
        onSelectOrUnselectAll = viewModel::toggleSelected
    )
}

@Composable
private fun RollOverCoinControlContent(
    uiState: RollOverCoinControlUiState = RollOverCoinControlUiState(),
    onTagCheckedChange: ((Int) -> Unit) = { _ -> },
    onCollectionCheckedChange: ((Int) -> Unit) = { _ -> },
    onContinueClicked: () -> Unit = { },
    onSelectOrUnselectAll: (isSelect: Boolean) -> Unit = {},
) {
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val isSelectAll = uiState.selectedCoinTags.size == uiState.tags.size &&
            uiState.selectedCoinCollections.size == uiState.collections.size

    NunchukTheme {
        NcScaffold(
            modifier = Modifier.systemBarsPadding(),
            topBar = {
                Box(
                    modifier = Modifier
                        .background(color = MaterialTheme.colorScheme.background)
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    IconButton(modifier = Modifier.align(Alignment.CenterStart), onClick = {
                        onBackPressedDispatcher?.onBackPressed()
                    }) {
                        Icon(
                            painter = painterResource(id = com.nunchuk.android.wallet.R.drawable.ic_back),
                            contentDescription = "Back icon"
                        )
                    }
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center,
                        text = stringResource(com.nunchuk.android.wallet.R.string.nc_collections),
                        style = NunchukTheme.typography.titleLarge
                    )
                    Text(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                            .clickable { onSelectOrUnselectAll(isSelectAll.not()) },
                        text = if (isSelectAll) stringResource(R.string.nc_unselect_all) else stringResource(
                            R.string.nc_select_all
                        ),
                        style = NunchukTheme.typography.title.copy(textDecoration = TextDecoration.Underline)
                    )
                }
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val transactionRequiredText = if (uiState.numOfTxs == 1) {
                        "transaction required"
                    } else {
                        "transactions required"
                    }
                    Text(
                        text = "${uiState.numOfTxs} $transactionRequiredText",
                        style = NunchukTheme.typography.bodySmall
                    )
                    Text(
                        modifier = Modifier.padding(top = 8.dp),
                        text = if (uiState.isCalculating) {
                            "Estimated fee: calculating..."
                        } else {
                            "Estimated fee: ${uiState.feeAmount.getBTCAmount()}"
                        },
                        style = NunchukTheme.typography.titleSmall
                    )
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth(),
                        onClick = {
                            onContinueClicked()
                        }) {
                        Text(text = stringResource(R.string.nc_text_continue))
                    }
                }

            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                LazyColumn {
                    item {
                        Text(
                            text = "Select the coin tags and collections you want to keep for the new wallet. Please note that the more tags and collections, the more transactions and fees will be required for the rollover.",
                            style = NunchukTheme.typography.body,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.padding(top = 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                modifier = Modifier
                                    .size(24.dp),
                                painter = painterResource(id = R.drawable.ic_coin_tag),
                                contentDescription = "Lock icon"
                            )
                            Text(
                                modifier = Modifier.padding(start = 8.dp),
                                text = "Tags",
                                style = NunchukTheme.typography.title
                            )
                        }
                    }

                    items(uiState.tags) { tag ->
                        TagItem(
                            modifier = Modifier.padding(vertical = 12.dp),
                            id = tag.coinTag.id,
                            name = tag.coinTag.name,
                            color = tag.coinTag.color,
                            numCoins = tag.numCoins,
                            checked = uiState.selectedCoinTags.contains(tag.coinTag.id),
                            tagFlow = TagFlow.ADD,
                            onCheckedChange = {
                                onTagCheckedChange(tag.coinTag.id)
                            }
                        )
                    }

                    item {
                        HorizontalDivider(
                            modifier = Modifier
                                .padding(vertical = 24.dp),
                            color = MaterialTheme.colorScheme.whisper,
                            thickness = 1.dp
                        )
                    }

                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                modifier = Modifier
                                    .size(24.dp),
                                painter = painterResource(id = R.drawable.ic_coin_collection),
                                contentDescription = "Lock icon"
                            )
                            Text(
                                modifier = Modifier.padding(start = 8.dp),
                                text = "Collections",
                                style = NunchukTheme.typography.title
                            )
                        }
                    }

                    items(uiState.collections) { collection ->
                        CollectionItem(
                            modifier = Modifier.padding(vertical = 12.dp),
                            id = collection.collection.id,
                            name = collection.collection.name,
                            numCoins = collection.numCoins,
                            checked = uiState.selectedCoinCollections.contains(collection.collection.id),
                            collectionFlow = CollectionFlow.ADD,
                            onCheckedChange = {
                                onCollectionCheckedChange(collection.collection.id)
                            })
                    }
                }
            }
        }
    }
}

@Composable
@Preview
private fun RollOverCoinControlScreenContentPreview() {
    RollOverCoinControlContent(
        uiState = RollOverCoinControlUiState(
            newWallet = Wallet(name = "new wallet name")
        )
    )
}