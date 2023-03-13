package com.nunchuk.android.wallet.components.coin.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.model.coin.CoinCard
import com.nunchuk.android.model.coin.CoinTag
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.coin.component.CoinListBottomBar
import com.nunchuk.android.wallet.components.coin.component.CoinListTopBarNoneMode
import com.nunchuk.android.wallet.components.coin.component.CoinListTopBarSelectMode
import com.nunchuk.android.wallet.components.coin.component.PreviewCoinCard
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CoinListFragment : Fragment() {
    private val viewModel: CoinListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CoinListScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect { event ->

            }
        }
    }
}

@Composable
private fun CoinListScreen(viewModel: CoinListViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CoinListContent(
        mode = state.mode,
        coins = state.coins,
        selectedCoin = state.selectedCoins,
        onSelectCoin = viewModel::onCoinSelect,
        onSelectOrUnselectAll = viewModel::onSelectOrUnselectAll,
        onSelectDone = viewModel::onSelectDone,
        enableSelectMode = viewModel::enableSelectMode,
        enableSearchMode = viewModel::enableSearchMode,
    )
}

@Composable
private fun CoinListContent(
    mode: CoinListMode = CoinListMode.NONE,
    coins: List<CoinCard> = emptyList(),
    selectedCoin: Set<CoinCard> = emptySet(),
    enableSelectMode: () -> Unit = {},
    enableSearchMode: () -> Unit = {},
    onSelectOrUnselectAll: (isSelect: Boolean) -> Unit = {},
    onSelectDone: () -> Unit = {},
    onSelectCoin: (coinCard: CoinCard, isSelected: Boolean) -> Unit = { _, _ -> }
) {
    NunchukTheme {
        Scaffold(topBar = {
            if (mode == CoinListMode.NONE) {
                CoinListTopBarNoneMode(enableSelectMode = enableSelectMode)
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
                LazyColumn(modifier = Modifier
                    .weight(1f)
                    .padding(innerPadding)) {
                    items(coins) { coin ->
                        PreviewCoinCard(
                            coinCard = coin,
                            onSelectCoin = onSelectCoin,
                            isSelected = selectedCoin.contains(coin),
                            selectable = mode == CoinListMode.SELECT
                        )
                    }
                }
                if (mode == CoinListMode.SELECT && selectedCoin.isNotEmpty()) {
                    CoinListBottomBar(
                        selectedCoin = selectedCoin
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun CoinListScreenPreview() {
    val coin = CoinCard(
        amount = "100,000 sats",
        isLock = true,
        isScheduleBroadCast = true,
        time = System.currentTimeMillis(),
        tags = listOf(
            CoinTag(Color.Blue.toArgb(), "Badcoins"),
            CoinTag(Color.Red.toArgb(), "Dirtycoins"),
            CoinTag(Color.Gray.toArgb(), "Dirty"),
            CoinTag(Color.Green.toArgb(), "Dirtys"),
            CoinTag(Color.DarkGray.toArgb(), "Dirtycoins"),
            CoinTag(Color.LightGray.toArgb(), "Dirtycoins"),
            CoinTag(Color.Magenta.toArgb(), "Dirtycoins"),
            CoinTag(Color.Cyan.toArgb(), "Dirtycoins"),
            CoinTag(Color.Black.toArgb(), "Dirtycoins"),
        ),
        note = "Send to Bob on Silk Road",
    )
    CoinListContent(
        coins = listOf(
            coin.copy(id = 1L),
            coin.copy(id = 2L),
            coin.copy(id = 3L),
            coin.copy(id = 4L),
            coin.copy(id = 5L)
        )
    )
}