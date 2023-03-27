package com.nunchuk.android.wallet.components.coin.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.wallet.CoinNavigationDirections
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.coin.base.BaseCoinListFragment
import com.nunchuk.android.wallet.components.coin.component.CoinListBottomBar
import com.nunchuk.android.wallet.components.coin.component.CoinListTopBarNoneMode
import com.nunchuk.android.wallet.components.coin.component.CoinListTopBarSelectMode
import com.nunchuk.android.wallet.components.coin.component.PreviewCoinCard
import com.nunchuk.android.wallet.components.coin.tag.TagFlow
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CoinListFragment : BaseCoinListFragment() {
    private val args: CoinListFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CoinListScreen(
                    viewModel = coinListViewModel,
                    args = args,
                    onViewCoinDetail = {
                        findNavController().navigate(
                            CoinNavigationDirections.actionGlobalCoinDetailFragment(
                                walletId = args.walletId,
                                txId = it.txid,
                                vout = it.vout
                            )
                        )
                    },
                    onSendBtc = {

                    },
                    onShowSelectedCoinMoreOption = {
                        showSelectCoinOptions()
                    },
                    onShowMoreOptions = {
                        showMoreOptions()
                    },
                    enableSearchMode = {
                        findNavController().navigate(
                            CoinListFragmentDirections.actionCoinListFragmentToCoinSearchNavigation(
                                walletId = args.walletId,
                            )
                        )
                    }
                )
            }
        }
    }

    override fun onOptionClicked(option: SheetOption) {
        super.onOptionClicked(option)
        when (option.type) {
            SheetOptionType.TYPE_VIEW_TAG -> findNavController().navigate(
                CoinNavigationDirections.actionGlobalCoinTagListFragment(
                    walletId = args.walletId,
                    tagFlow = TagFlow.VIEW,
                    coins = emptyArray()
                )
            )
            SheetOptionType.TYPE_VIEW_COLLECTION -> Unit
            SheetOptionType.TYPE_VIEW_LOCKED_COIN -> findNavController().navigate(
                CoinListFragmentDirections.actionCoinListFragmentSelf(
                    walletId = args.walletId,
                    listType = CoinListType.LOCKED
                )
            )
            SheetOptionType.TYPE_REMOVE_COIN_FROM_TAG -> {
                coinListViewModel.removeCoin(args.walletId, args.tagId)
            }
        }
    }

    override fun showSelectCoinOptions() {
        if (args.tagId > 0) {
            showRemoveCoinFromTag()
        } else {
            super.showSelectCoinOptions()
        }
    }

    private fun showRemoveCoinFromTag() {
        BottomSheetOption.newInstance(listOf(
            SheetOption(
                type = SheetOptionType.TYPE_REMOVE_COIN_FROM_TAG,
                label = getString(R.string.nc_remove_coin_from_this_tag)
            ),
        )).show(childFragmentManager, "BottomSheetOption")
    }

    private fun showMoreOptions() {
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_VIEW_TAG,
                    label = getString(R.string.nc_view_tag)
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_VIEW_COLLECTION,
                    label = getString(R.string.nc_view_collection)
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_VIEW_LOCKED_COIN,
                    label = getString(R.string.nc_view_locked_coin)
                ),
            )
        ).show(childFragmentManager, "BottomSheetOption")
    }

    override val walletId: String
        get() = args.walletId

    override fun getSelectedCoins(): List<UnspentOutput> {
        return coinListViewModel.getSelectedCoins()
    }

    override fun resetSelect() {
        coinListViewModel.resetSelect()
    }
}

@Composable
private fun CoinListScreen(
    viewModel: CoinListViewModel = viewModel(),
    onViewCoinDetail: (output: UnspentOutput) -> Unit = {},
    onShowSelectedCoinMoreOption: () -> Unit = {},
    onSendBtc: () -> Unit = {},
    onShowMoreOptions: () -> Unit = {},
    enableSearchMode: () -> Unit = {},
    args: CoinListFragmentArgs,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val filterCoins = when {
        args.tagId > 0 -> state.coins.filter { it.tags.contains(args.tagId) }
        args.txId.isNotEmpty() -> state.coins.filter { it.txid == args.txId }
        args.listType == CoinListType.LOCKED -> state.coins.filter { it.isLocked }
        else -> state.coins
    }

    CoinListContent(
        mode = state.mode,
        type = args.listType,
        coins = filterCoins,
        tags = state.tags,
        selectedCoin = state.selectedCoins,
        onSelectCoin = viewModel::onCoinSelect,
        onSelectOrUnselectAll = viewModel::onSelectOrUnselectAll,
        onSelectDone = viewModel::onSelectDone,
        enableSelectMode = viewModel::enableSelectMode,
        enableSearchMode = enableSearchMode,
        onViewCoinDetail = onViewCoinDetail,
        onShowSelectedCoinMoreOption = onShowSelectedCoinMoreOption,
        onSendBtc = onSendBtc,
        onShowMoreOptions = onShowMoreOptions,
    )
}

@Composable
private fun CoinListContent(
    mode: CoinListMode = CoinListMode.NONE,
    type: CoinListType = CoinListType.ALL,
    coins: List<UnspentOutput> = emptyList(),
    tags: Map<Int, CoinTag> = emptyMap(),
    selectedCoin: Set<UnspentOutput> = emptySet(),
    enableSelectMode: () -> Unit = {},
    enableSearchMode: () -> Unit = {},
    onSelectOrUnselectAll: (isSelect: Boolean) -> Unit = {},
    onSelectDone: () -> Unit = {},
    onViewCoinDetail: (output: UnspentOutput) -> Unit = {},
    onSendBtc: () -> Unit = {},
    onShowSelectedCoinMoreOption: () -> Unit = {},
    onShowMoreOptions: () -> Unit = {},
    onSelectCoin: (output: UnspentOutput, isSelected: Boolean) -> Unit = { _, _ -> }
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding(),
            topBar = {
                if (mode == CoinListMode.NONE) {
                    CoinListTopBarNoneMode(
                        enableSelectMode = enableSelectMode,
                        onShowMoreOptions = onShowMoreOptions,
                        isShowMore = type == CoinListType.ALL,
                        title = if (type == CoinListType.ALL)
                            stringResource(R.string.nc_coins)
                        else stringResource(R.string.nc_locked_coin)
                    )
                } else if (mode == CoinListMode.SELECT) {
                    CoinListTopBarSelectMode(
                        isSelectAll = coins.size == selectedCoin.size,
                        onSelectOrUnselectAll = onSelectOrUnselectAll,
                        onSelectDone = onSelectDone
                    )
                }
            }, floatingActionButton = {
                if (mode == CoinListMode.NONE) {
                    FloatingActionButton(onClick = enableSearchMode) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_search_white),
                            contentDescription = "Search"
                        )
                    }
                }
            }) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(coins) { coin ->
                        PreviewCoinCard(
                            output = coin,
                            onSelectCoin = onSelectCoin,
                            isSelected = selectedCoin.contains(coin),
                            selectable = mode == CoinListMode.SELECT,
                            onViewCoinDetail = onViewCoinDetail,
                            tags = tags,
                        )
                    }
                }
                if (mode == CoinListMode.SELECT && selectedCoin.isNotEmpty()) {
                    CoinListBottomBar(
                        selectedCoin = selectedCoin,
                        onSendBtc = onSendBtc,
                        onShowSelectedCoinMoreOption = onShowSelectedCoinMoreOption,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun CoinListScreenPreview() {
    val coin = UnspentOutput(
        amount = Amount(1000000L),
        isLocked = true,
        scheduleTime = System.currentTimeMillis(),
        time = System.currentTimeMillis(),
        tags = setOf(),
        memo = "Send to Bob on Silk Road",
        status = TransactionStatus.PENDING_CONFIRMATION
    )
    CoinListContent(
        coins = listOf(
            coin.copy(vout = 1),
            coin.copy(vout = 2),
            coin.copy(vout = 3),
            coin.copy(vout = 4),
            coin.copy(vout = 5)
        )
    )
}