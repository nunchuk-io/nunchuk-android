package com.nunchuk.android.wallet.components.coin.detail.ancestry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.type.CoinStatus
import com.nunchuk.android.wallet.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CoinAncestryFragment : Fragment() {
    private val viewModel: CoinAncestryViewModel by viewModels()
    private val args: CoinAncestryFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CoinAncestryScreen(
                    viewModel = viewModel,
                    onCoinClick = { coin ->
                        findNavController().navigate(
                            CoinAncestryFragmentDirections.actionGlobalCoinDetailFragment(
                                walletId = args.walletId,
                                output = coin,
                                isSpent = true
                            )
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun CoinAncestryScreen(
    viewModel: CoinAncestryViewModel = viewModel(),
    onCoinClick: (output: UnspentOutput) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CoinAncestryContent(allCoins = state.coins, onCoinClick = onCoinClick)
}

@Composable
private fun CoinAncestryContent(
    allCoins: List<List<UnspentOutput>> = emptyList(),
    onCoinClick: (output: UnspentOutput) -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_coin_ancestry),
                    textStyle = NunchukTheme.typography.titleLarge
                )
            }) { innerPadding ->
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                itemsIndexed(allCoins) { index, coins ->
                    CoinAncestryRow(
                        showTopLine = index > 0,
                        showBottomLine = index < allCoins.lastIndex,
                        coins = coins,
                        isRootCoin = index == 0,
                        onCoinClick = onCoinClick
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun CoinAncestryScreenPreview() {
    val coin = UnspentOutput(
        amount = Amount(1000000L),
        isLocked = true,
        scheduleTime = System.currentTimeMillis(),
        time = System.currentTimeMillis(),
        tags = setOf(),
        memo = "Send to Bob on Silk Road",
        status = CoinStatus.OUTGOING_PENDING_CONFIRMATION
    )
    val allCoins = listOf(
        listOf(coin),
        listOf(
            coin.copy(vout = 1),
            coin.copy(vout = 2),
            coin.copy(vout = 3),
            coin.copy(vout = 4),
            coin.copy(vout = 5)
        ),
        listOf(
            coin.copy(vout = 1),
            coin.copy(vout = 2),
            coin.copy(vout = 3),
            coin.copy(vout = 4),
            coin.copy(vout = 5)
        ),
    )
    CoinAncestryContent(
        allCoins = allCoins
    )
}