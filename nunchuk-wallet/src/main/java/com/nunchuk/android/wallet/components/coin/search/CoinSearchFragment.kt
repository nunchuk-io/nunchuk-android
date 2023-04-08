package com.nunchuk.android.wallet.components.coin.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
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
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.wallet.CoinNavigationDirections
import com.nunchuk.android.wallet.components.coin.base.BaseCoinListFragment
import com.nunchuk.android.wallet.components.coin.component.CoinListBottomBar
import com.nunchuk.android.wallet.components.coin.component.CoinListTopBarSelectMode
import com.nunchuk.android.wallet.components.coin.detail.component.TagHorizontalList
import com.nunchuk.android.wallet.components.coin.filter.CoinFilterFragment
import com.nunchuk.android.wallet.components.coin.filter.CoinFilterFragmentArgs
import com.nunchuk.android.wallet.components.coin.list.CoinListMode
import com.nunchuk.android.wallet.components.coin.list.CoinListViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
                    onViewAll = {
                        findNavController().navigate(
                            CoinNavigationDirections.actionGlobalCoinTagListFragment(
                                args.walletId,
                                TagFlow.NONE,
                                emptyArray()
                            )
                        )
                    }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->

                }
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
    viewModel: CoinSearchViewModel = viewModel(),
    coinListViewModel: CoinListViewModel = viewModel(),
    onFilterClicked: () -> Unit = {},
    onViewCoinDetail: (output: UnspentOutput) -> Unit = {},
    onSendBtc: () -> Unit = {},
    onViewAll: () -> Unit,
    onShowSelectedCoinMoreOption: () -> Unit = {},
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val coinListUiState by coinListViewModel.state.collectAsStateWithLifecycle()
    CoinSearchFragmentContent(
        tags = coinListUiState.tags,
        coins = uiState.coins,
        mode = uiState.mode,
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
        onViewAll = onViewAll,
        onShowSelectedCoinMoreOption = onShowSelectedCoinMoreOption
    )
}

@Composable
private fun CoinSearchFragmentContent(
    onFilterClicked: () -> Unit = {},
    enableSelectMode: () -> Unit = {},
    queryState: MutableState<String> = mutableStateOf(""),
    handleSearch: suspend (query: String) -> Unit = {},
    tags: Map<Int, CoinTag> = emptyMap(),
    coins: List<UnspentOutput> = emptyList(),
    mode: CoinListMode = CoinListMode.NONE,
    selectedCoins: Set<UnspentOutput> = emptySet(),
    onViewCoinDetail: (output: UnspentOutput) -> Unit = {},
    onSelectCoin: (output: UnspentOutput, isSelected: Boolean) -> Unit = { _, _ -> },
    onSelectOrUnselectAll: (isSelect: Boolean) -> Unit = {},
    onSelectDone: () -> Unit = {},
    onSendBtc: () -> Unit = {},
    onViewAll: () -> Unit = {},
    onShowSelectedCoinMoreOption: () -> Unit = {},
) {
    val onBackPressOwner = LocalOnBackPressedDispatcherOwner.current
    var query by remember { queryState }

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
                if (mode == CoinListMode.NONE) {
                    SearchCoinTopAppBar(
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
                        onSelectOrUnselectAll = onSelectOrUnselectAll,
                        onSelectDone = onSelectDone
                    )
                }
            },
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                if (query.isBlank()) {
                    TagHorizontalList(tags = tags.values.toList(), onViewAll = onViewAll)
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(coins) { coin ->
                            PreviewCoinCard(
                                output = coin,
                                onSelectCoin = onSelectCoin,
                                isSelected = selectedCoins.contains(coin),
                                mode = if (mode == CoinListMode.SELECT) MODE_SELECT else MODE_VIEW_DETAIL,
                                onViewCoinDetail = onViewCoinDetail,
                                tags = tags,
                            )
                        }
                    }
                    if (mode == CoinListMode.SELECT && selectedCoins.isNotEmpty()) {
                        CoinListBottomBar(
                            selectedCoin = selectedCoins,
                            onSendBtc = onSendBtc,
                            onShowSelectedCoinMoreOption = onShowSelectedCoinMoreOption,
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
    CoinSearchFragmentContent(

    )
}