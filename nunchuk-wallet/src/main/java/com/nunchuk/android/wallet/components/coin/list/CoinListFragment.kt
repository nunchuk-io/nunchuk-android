package com.nunchuk.android.wallet.components.coin.list

import android.app.Activity
import android.content.Intent
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.*
import com.nunchuk.android.core.coin.TagFlow
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.fromSATtoBTC
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.type.CoinStatus
import com.nunchuk.android.wallet.CoinNavigationDirections
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.coin.base.BaseCoinListFragment
import com.nunchuk.android.wallet.components.coin.collection.CoinCollectionListFragment
import com.nunchuk.android.wallet.components.coin.collection.CollectionFlow
import com.nunchuk.android.wallet.components.coin.component.CoinListBottomBar
import com.nunchuk.android.wallet.components.coin.component.CoinListTopBarNoneMode
import com.nunchuk.android.wallet.components.coin.component.CoinListTopBarSelectMode
import com.nunchuk.android.wallet.components.coin.component.SelectCoinCreateTransactionBottomBar
import com.nunchuk.android.wallet.components.coin.tag.CoinTagSelectColorBottomSheetFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CoinListFragment : BaseCoinListFragment() {
    private val args: CoinListFragmentArgs by navArgs()

    @Inject
    lateinit var navigator: NunchukNavigator

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CoinListScreen(
                    viewModel = coinListViewModel, args = args,
                    onViewCoinDetail = {
                        findNavController().navigate(
                            CoinNavigationDirections.actionGlobalCoinDetailFragment(
                                walletId = args.walletId, txId = it.txid, vout = it.vout
                            )
                        )
                    },
                    onSendBtc = {
                        navigator.openInputAmountScreen(
                            activityContext = requireActivity(),
                            walletId = args.walletId,
                            inputs = coinListViewModel.getSelectedCoins(),
                            availableAmount = coinListViewModel.getSelectedCoins()
                                .sumOf { it.amount.value }.toDouble().fromSATtoBTC()
                        )
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
                    },
                    onUseCoinClicked = {
                        requireActivity().setResult(Activity.RESULT_OK, Intent().apply {
                            putParcelableArrayListExtra(GlobalResultKey.EXTRA_COINS, ArrayList(coinListViewModel.getSelectedCoins()))
                        })
                        requireActivity().finish()
                    },
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        args.inputs?.let { inputs ->
            coinListViewModel.setSelectedCoin(inputs)
        }
        setFragmentResultListener(CoinCollectionListFragment.REQUEST_KEY) { _, _ ->
            findNavController().popBackStack()
            clearFragmentResult(CoinTagSelectColorBottomSheetFragment.REQUEST_KEY)
        }
    }

    override fun onOptionClicked(option: SheetOption) {
        super.onOptionClicked(option)
        when (option.type) {
            SheetOptionType.TYPE_VIEW_TAG -> findNavController().navigate(
                CoinNavigationDirections.actionGlobalCoinTagListFragment(
                    walletId = args.walletId, tagFlow = TagFlow.VIEW, coins = emptyArray()
                )
            )

            SheetOptionType.TYPE_VIEW_COLLECTION -> {
                findNavController().navigate(
                    CoinNavigationDirections.actionGlobalCoinCollectionListFragment(
                        walletId = args.walletId,
                        collectionFlow = CollectionFlow.VIEW,
                        coins = emptyArray()
                    )
                )
            }

            SheetOptionType.TYPE_VIEW_LOCKED_COIN -> findNavController().navigate(
                CoinListFragmentDirections.actionCoinListFragmentSelf(
                    walletId = args.walletId,
                    listType = CoinListType.LOCKED,
                )
            )

            SheetOptionType.TYPE_REMOVE_COIN_FROM_TAG -> {
                coinListViewModel.removeCoinFromTag(args.walletId, args.tagId)
            }

            SheetOptionType.TYPE_REMOVE_COIN_FROM_COLLECTION -> {
                coinListViewModel.removeCoinFromCollection(args.walletId, args.collectionId)
            }

            SheetOptionType.TYPE_MOVE_COIN_TO_COLLECTION -> {
                findNavController().navigate(
                    CoinNavigationDirections.actionGlobalCoinCollectionListFragment(
                        walletId = walletId,
                        collectionFlow = CollectionFlow.MOVE,
                        coins = getSelectedCoins().toTypedArray()
                    )
                )
            }
        }
    }

    override fun showSelectCoinOptions() {
        if (args.tagId > 0) {
            showRemoveCoinFromTag()
        } else if (args.collectionId > 0) {
            showOptionCoinFromCollection()
        } else {
            super.showSelectCoinOptions()
        }
    }

    private fun showRemoveCoinFromTag() {
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_REMOVE_COIN_FROM_TAG,
                    label = getString(R.string.nc_remove_coin_from_this_tag)
                ),
            )
        ).show(childFragmentManager, "BottomSheetOption")
    }

    private fun showOptionCoinFromCollection() {
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_REMOVE_COIN_FROM_COLLECTION,
                    label = getString(R.string.nc_remove_coin_from_this_collection)
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_MOVE_COIN_TO_COLLECTION,
                    label = getString(R.string.nc_move_coin_to_another_collection)
                )
            )
        ).show(childFragmentManager, "BottomSheetOption")
    }

    private fun showMoreOptions() {
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_VIEW_TAG, label = getString(R.string.nc_view_tag)
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
    onUseCoinClicked: () -> Unit = {},
    args: CoinListFragmentArgs,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val filterCoins by remember {
        derivedStateOf {
            when {
                args.tagId > 0 -> state.coins.filter { it.tags.contains(args.tagId) }
                args.collectionId > 0 -> state.coins.filter { it.collection.contains(args.collectionId) }
                args.txId.isNotEmpty() -> state.coins.filter { it.txid == args.txId }
                args.listType == CoinListType.LOCKED -> state.coins.filter { it.isLocked }
                else -> state.coins
            }
        }
    }

    CoinListContent(
        mode = if (!args.inputs.isNullOrEmpty()) CoinListMode.TRANSACTION_SELECT else state.mode,
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
        onUseCoinClicked = onUseCoinClicked,
        amount = args.amount ?: Amount()
    )
}

@Composable
private fun CoinListContent(
    mode: CoinListMode = CoinListMode.NONE,
    type: CoinListType = CoinListType.ALL,
    amount: Amount = Amount(),
    coins: List<UnspentOutput> = emptyList(),
    tags: Map<Int, CoinTag> = emptyMap(),
    selectedCoin: Set<UnspentOutput> = emptySet(),
    enableSelectMode: () -> Unit = {},
    enableSearchMode: () -> Unit = {},
    onSelectOrUnselectAll: (isSelect: Boolean) -> Unit = {},
    onSelectDone: () -> Unit = {},
    onViewCoinDetail: (output: UnspentOutput) -> Unit = {},
    onSendBtc: () -> Unit = {},
    onUseCoinClicked: () -> Unit = {},
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
                when (mode) {
                    CoinListMode.NONE -> {
                        CoinListTopBarNoneMode(
                            enableSelectMode = enableSelectMode,
                            onShowMoreOptions = onShowMoreOptions,
                            isShowMore = type == CoinListType.ALL,
                            title = if (type == CoinListType.ALL)
                                stringResource(R.string.nc_coins)
                            else stringResource(R.string.nc_locked_coin)
                        )
                    }

                    CoinListMode.SELECT -> {
                        CoinListTopBarSelectMode(
                            isSelectAll = coins.size == selectedCoin.size,
                            onSelectOrUnselectAll = onSelectOrUnselectAll,
                            onSelectDone = onSelectDone
                        )
                    }

                CoinListMode.TRANSACTION_SELECT -> (NcTopAppBar(title = ""))
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
                            mode = if (mode == CoinListMode.SELECT || mode == CoinListMode.TRANSACTION_SELECT) MODE_SELECT else MODE_VIEW_DETAIL,
                            onViewCoinDetail = onViewCoinDetail,
                            tags = tags,
                        )
                    }
                }
                if (selectedCoin.isNotEmpty()) {
                    if (mode == CoinListMode.SELECT) {
                        CoinListBottomBar(
                            selectedCoin = selectedCoin,
                            onSendBtc = onSendBtc,
                            onShowSelectedCoinMoreOption = onShowSelectedCoinMoreOption,
                        )
                    } else if (mode == CoinListMode.TRANSACTION_SELECT) {
                        SelectCoinCreateTransactionBottomBar(
                            selectedCoin = selectedCoin,
                            onUseCoinClicked = onUseCoinClicked,
                            amount = amount
                        )
                    }
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
        status = CoinStatus.OUTGOING_PENDING_CONFIRMATION
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