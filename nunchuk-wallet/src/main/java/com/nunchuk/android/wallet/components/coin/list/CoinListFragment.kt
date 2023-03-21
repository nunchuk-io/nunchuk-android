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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.coin.component.CoinListBottomBar
import com.nunchuk.android.wallet.components.coin.component.CoinListTopBarNoneMode
import com.nunchuk.android.wallet.components.coin.component.CoinListTopBarSelectMode
import com.nunchuk.android.wallet.components.coin.component.PreviewCoinCard
import com.nunchuk.android.wallet.components.coin.tag.TagFlow
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CoinListFragment : Fragment(), BottomSheetOptionListener {
    private val args: CoinListFragmentArgs by navArgs()
    private val viewModel: CoinListViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CoinListScreen(
                    viewModel = viewModel,
                    args = args,
                    onViewCoinDetail = {
                        findNavController().navigate(
                            CoinListFragmentDirections.actionCoinListFragmentToCoinDetailFragment(
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
                    }
                )
            }
        }
    }

    override fun onOptionClicked(option: SheetOption) {
        when (option.type) {
            SheetOptionType.TYPE_LOCK_COIN -> viewModel.onLockCoin()
            SheetOptionType.TYPE_UNLOCK_COIN -> viewModel.onUnlockCoin()
            SheetOptionType.TYPE_ADD_COLLECTION -> TODO()
            SheetOptionType.TYPE_ADD_TAG -> findNavController().navigate(
                CoinListFragmentDirections.actionCoinListFragmentToCoinTagListFragment(
                    walletId = args.walletId,
                    tagFlow = TagFlow.ADD,
                    coins = viewModel.getSelectedCoins().toTypedArray()
                )
            )
            SheetOptionType.TYPE_VIEW_TAG -> findNavController().navigate(
                CoinListFragmentDirections.actionCoinListFragmentToCoinTagListFragment(
                    walletId = args.walletId,
                    tagFlow = TagFlow.VIEW,
                    coins = emptyArray()
                )
            )
            SheetOptionType.TYPE_VIEW_COLLECTION -> TODO()
            SheetOptionType.TYPE_VIEW_LOCKED_COIN -> findNavController().navigate(
                CoinListFragmentDirections.actionCoinListFragmentSelf(
                    walletId = args.walletId,
                    listType = CoinListType.LOCKED
                )
            )
        }
    }

    private fun showSelectCoinOptions() {
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_LOCK_COIN,
                    label = getString(R.string.nc_lock_coin)
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_UNLOCK_COIN,
                    label = getString(R.string.nc_unlock_coin)
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_COLLECTION,
                    label = getString(R.string.nc_add_to_a_collection)
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_TAG,
                    label = getString(R.string.nc_add_tags)
                ),
            )
        ).show(childFragmentManager, "BottomSheetOption")
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect { event ->
                when (event) {
                    is CoinListEvent.Loading -> showOrHideLoading(event.isLoading)
                    CoinListEvent.CoinLocked -> showSuccess(getString(R.string.nc_coin_locked))
                    CoinListEvent.CoinUnlocked -> showSuccess(getString(R.string.nc_coin_unlocked))
                }
            }
        }
    }
}

@Composable
private fun CoinListScreen(
    viewModel: CoinListViewModel = viewModel(),
    onViewCoinDetail: (output: UnspentOutput) -> Unit = {},
    onShowSelectedCoinMoreOption: () -> Unit = {},
    onSendBtc: () -> Unit = {},
    onShowMoreOptions: () -> Unit = {},
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
        enableSearchMode = viewModel::enableSearchMode,
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
                            stringResource(R.string.nc_coin)
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
            Column {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(innerPadding)
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