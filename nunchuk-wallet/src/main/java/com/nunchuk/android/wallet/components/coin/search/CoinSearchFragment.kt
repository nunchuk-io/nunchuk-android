package com.nunchuk.android.wallet.components.coin.search

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.MODE_SELECT
import com.nunchuk.android.compose.MODE_VIEW_DETAIL
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.PreviewCoinCard
import com.nunchuk.android.core.coin.TagFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.fromSATtoBTC
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.wallet.CoinNavigationDirections
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.coin.base.BaseCoinListFragment
import com.nunchuk.android.wallet.components.coin.collection.CollectionFlow
import com.nunchuk.android.wallet.components.coin.component.CoinListBottomBar
import com.nunchuk.android.wallet.components.coin.component.CoinListTopBarSelectMode
import com.nunchuk.android.wallet.components.coin.component.SelectCoinCreateTransactionBottomBar
import com.nunchuk.android.wallet.components.coin.component.ViewSelectedCoinList
import com.nunchuk.android.wallet.components.coin.detail.component.CollectionHorizontalList
import com.nunchuk.android.wallet.components.coin.detail.component.TagHorizontalList
import com.nunchuk.android.wallet.components.coin.filter.CoinFilterFragment
import com.nunchuk.android.wallet.components.coin.filter.CoinFilterFragmentArgs
import com.nunchuk.android.wallet.components.coin.list.CoinListMode
import com.nunchuk.android.wallet.components.coin.list.CoinListViewModel
import com.nunchuk.android.widget.NCInfoDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
class CoinSearchFragment : BaseCoinListFragment() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val args: CoinSearchFragmentArgs by navArgs()
    private val viewModel: CoinSearchViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CoinSearchFragmentScreen(
                    args = args,
                    viewModel = viewModel,
                    coinListViewModel = coinListViewModel,
                    onFilterClicked = {
                        findNavController().navigate(
                            CoinSearchFragmentDirections.actionCoinSearchFragmentFragmentToCoinFilterFragment(
                                viewModel.filter.value
                            )
                        )
                    },
                    onViewCoinDetail = {
                        findNavController().navigate(
                            CoinNavigationDirections.actionGlobalCoinDetailFragment(
                                walletId = args.walletId,
                                txId = it.txid,
                                vout = it.vout
                            )
                        )
                    },
                    onShowSelectedCoinMoreOption = {
                        showSelectCoinOptions()
                    },
                    onSendBtc = {
                        navigator.openInputAmountScreen(
                            activityContext = requireActivity(),
                            walletId = args.walletId,
                            inputs = viewModel.getSelectedCoins(),
                            availableAmount = viewModel.getSelectedCoins()
                                .sumOf { it.amount.value }.toDouble().fromSATtoBTC()
                        )
                    },
                    onViewAllTags = {
                        findNavController().navigate(
                            CoinNavigationDirections.actionGlobalCoinTagListFragment(
                                args.walletId,
                                TagFlow.NONE,
                                emptyArray()
                            )
                        )
                    },
                    onViewAllCollections = {
                        findNavController().navigate(
                            CoinNavigationDirections.actionGlobalCoinCollectionListFragment(
                                args.walletId,
                                CollectionFlow.VIEW,
                                emptyArray()
                            )
                        )
                    },
                    onUseCoinClicked = {
                        val selectedCoins = viewModel.getSelectedCoins()
                        if (selectedCoins.any { it.isLocked }) {
                            NCInfoDialog(requireActivity())
                                .showDialog(message = getString(R.string.nc_locked_coin_can_not_used))
                        } else {
                            requireActivity().setResult(Activity.RESULT_OK, Intent().apply {
                                putParcelableArrayListExtra(
                                    GlobalResultKey.EXTRA_COINS,
                                    ArrayList(viewModel.getSelectedCoins())
                                )
                            })
                            requireActivity().finish()
                        }
                    },
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        args.inputs?.let { inputs ->
            viewModel.setSelectedCoin(inputs)
        }

        setFragmentResultListener(CoinFilterFragment.REQUEST_KEY) { _, bundle ->
            val filter = CoinFilterFragmentArgs.fromBundle(bundle)
            viewModel.updateFilter(filter.filter)
            clearFragmentResult(CoinFilterFragment.REQUEST_KEY)
        }

        flowObserver(coinListViewModel.state) { state ->
            viewModel.update(state.coins, state.tags, state.collections)
        }
    }

    override val walletId: String
        get() = args.walletId

    override fun getSelectedCoins(): List<UnspentOutput> =
        viewModel.state.value.selectedCoins.toList()

    override fun resetSelect() = viewModel.resetSelect()
}

@Composable
private fun CoinSearchFragmentScreen(
    args: CoinSearchFragmentArgs,
    viewModel: CoinSearchViewModel = viewModel(),
    coinListViewModel: CoinListViewModel = viewModel(),
    onFilterClicked: () -> Unit = {},
    onViewCoinDetail: (output: UnspentOutput) -> Unit = {},
    onSendBtc: () -> Unit = {},
    onViewAllTags: () -> Unit,
    onViewAllCollections: () -> Unit = {},
    onShowSelectedCoinMoreOption: () -> Unit = {},
    onUseCoinClicked: () -> Unit = {},
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val coinListUiState by coinListViewModel.state.collectAsStateWithLifecycle()
    CoinSearchFragmentContent(
        tags = coinListUiState.tags,
        collections = coinListUiState.collections,
        coins = uiState.coins,
        mode = if (!args.inputs.isNullOrEmpty()) CoinListMode.TRANSACTION_SELECT else uiState.mode,
        queryState = viewModel.queryState,
        selectedCoins = uiState.selectedCoins,
        handleSearch = viewModel::handleSearch,
        enableSelectMode = viewModel::enableSelectMode,
        onFilterClicked = onFilterClicked,
        onSelectCoin = viewModel::onCoinSelect,
        onViewCoinDetail = onViewCoinDetail,
        onSelectDone = viewModel::onSelectDone,
        onSelectOrUnselectAll = viewModel::onSelectOrUnselectAll,
        onSendBtc = onSendBtc,
        onViewAllTags = onViewAllTags,
        onViewAllCollections = onViewAllCollections,
        onShowSelectedCoinMoreOption = onShowSelectedCoinMoreOption,
        onUseCoinClicked = onUseCoinClicked,
        amount = args.amount ?: Amount(),
        isFiltering = viewModel.isFiltering
    )
}

@Composable
private fun CoinSearchFragmentContent(
    amount: Amount = Amount(),
    isFiltering: Boolean = false,
    onFilterClicked: () -> Unit = {},
    enableSelectMode: () -> Unit = {},
    queryState: MutableState<String> = mutableStateOf(""),
    handleSearch: suspend (query: String) -> Unit = {},
    tags: Map<Int, CoinTag> = emptyMap(),
    collections: Map<Int, CoinCollection> = emptyMap(),
    coins: List<UnspentOutput> = emptyList(),
    mode: CoinListMode = CoinListMode.NONE,
    selectedCoins: Set<UnspentOutput> = emptySet(),
    onViewCoinDetail: (output: UnspentOutput) -> Unit = {},
    onSelectCoin: (output: UnspentOutput, isSelected: Boolean) -> Unit = { _, _ -> },
    onSelectOrUnselectAll: (isSelect: Boolean, coins: List<UnspentOutput>) -> Unit = { _, _ -> },
    onSelectDone: () -> Unit = {},
    onSendBtc: () -> Unit = {},
    onViewAllTags: () -> Unit = {},
    onViewAllCollections: () -> Unit = {},
    onShowSelectedCoinMoreOption: () -> Unit = {},
    onUseCoinClicked: () -> Unit = {},
) {
    val onBackPressOwner = LocalOnBackPressedDispatcherOwner.current
    var query by remember { queryState }
    var selectedTransactionCoinVisible by remember { mutableStateOf(false) }
    var previewSelectedCoins by remember { mutableStateOf(emptyList<UnspentOutput>()) }

    LaunchedEffect(query) {
        delay(300L)
        handleSearch(query)
    }

    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding(),
            topBar = {
                if (mode == CoinListMode.NONE || mode == CoinListMode.TRANSACTION_SELECT) {
                    SearchCoinTopAppBar(
                        modifier = Modifier.padding(top = 8.dp),
                        onBackPressOwner = onBackPressOwner,
                        query = query,
                        isEmpty = coins.isEmpty(),
                        onQueryChange = {
                            query = it
                        },
                        enableSelectMode = enableSelectMode,
                        onFilterClicked = onFilterClicked,
                    )
                } else if (mode == CoinListMode.SELECT) {
                    CoinListTopBarSelectMode(
                        isSelectAll = coins.size == selectedCoins.size,
                        onSelectOrUnselectAll = { isSelect ->
                            onSelectOrUnselectAll(
                                isSelect,
                                coins
                            )
                        },
                        onSelectDone = onSelectDone
                    )
                }
            },
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                if (query.isBlank() && mode != CoinListMode.TRANSACTION_SELECT) {
                    TagHorizontalList(tags = tags.values.toList(), onViewAll = onViewAllTags)
                    CollectionHorizontalList(
                        collections = collections.values.toList(),
                        onViewAll = onViewAllCollections
                    )
                } else {
                    Box(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (coins.isNotEmpty()) {
                                if (mode != CoinListMode.SELECT && isFiltering) {
                                    Text(
                                        modifier = Modifier.padding(
                                            horizontal = 16.dp,
                                            vertical = 8.dp
                                        ),
                                        text = stringResource(
                                            R.string.nc_results_found,
                                            coins.size
                                        ),
                                        style = NunchukTheme.typography.body
                                    )
                                }
                                LazyColumn {
                                    items(coins) { coin ->
                                        PreviewCoinCard(
                                            output = coin,
                                            onSelectCoin = onSelectCoin,
                                            isSelected = selectedCoins.contains(coin),
                                            mode = if (mode == CoinListMode.SELECT || mode == CoinListMode.TRANSACTION_SELECT) MODE_SELECT else MODE_VIEW_DETAIL,
                                            onViewCoinDetail = onViewCoinDetail,
                                            tags = tags,
                                        )
                                    }
                                }
                            } else if (isFiltering) {
                                EmptySearchState()
                            }
                        }
                        androidx.compose.animation.AnimatedVisibility(
                            modifier = Modifier.align(
                                Alignment.BottomCenter
                            ), visible = selectedTransactionCoinVisible,
                            enter = slideInVertically() + fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                            exit = slideOutVertically() + fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
                        ) {
                            ViewSelectedCoinList(
                                allTags = tags,
                                selectedCoin = selectedCoins,
                                coins = previewSelectedCoins,
                                onSelectCoin = onSelectCoin,
                                onSelectOrUnselectAll = onSelectOrUnselectAll
                            )
                        }
                    }
                    if (mode == CoinListMode.SELECT && selectedCoins.isNotEmpty()) {
                        CoinListBottomBar(
                            selectedCoin = selectedCoins,
                            onSendBtc = onSendBtc,
                            onShowSelectedCoinMoreOption = onShowSelectedCoinMoreOption,
                        )
                    } else if ((mode == CoinListMode.TRANSACTION_SELECT && selectedCoins.isNotEmpty()) || selectedTransactionCoinVisible) {
                        SelectCoinCreateTransactionBottomBar(
                            isExpand = selectedTransactionCoinVisible,
                            selectedCoin = selectedCoins,
                            onUseCoinClicked = onUseCoinClicked,
                            amount = amount,
                            onViewSelectedTransactionCoin = {
                                selectedTransactionCoinVisible = !selectedTransactionCoinVisible
                                previewSelectedCoins = selectedCoins.toList()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun CoinSearchFragmentScreenPreview() {
    CoinSearchFragmentContent()
}