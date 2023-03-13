package com.nunchuk.android.wallet.components.coin.list

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import com.nunchuk.android.model.coin.CoinCard
import com.nunchuk.android.model.coin.CoinTag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class CoinListViewModel @Inject constructor(

) : ViewModel() {
    private val _state = MutableStateFlow(CoinListUiState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<CoinListEvent>()
    val event = _event.asSharedFlow()

    init {
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
        val coins = listOf(
            coin.copy(id = 1L),
            coin.copy(id = 2L),
            coin.copy(id = 3L),
            coin.copy(id = 4L),
            coin.copy(id = 5L)
        )
        _state.update {
            it.copy(coins = coins)
        }
    }

    fun enableSelectMode() {
        _state.update { it.copy(mode = CoinListMode.SELECT) }
    }

    fun enableSearchMode() {
        _state.update { it.copy(mode = CoinListMode.SEARCH) }
    }

    fun onSelectOrUnselectAll(isSelect: Boolean) {
        _state.update {
            it.copy(
                selectedCoins = if (isSelect) state.value.coins.toSet() else emptySet()
            )
        }
    }

    fun onSelectDone() {
        _state.update { it.copy(mode = CoinListMode.NONE) }
    }

    fun onCoinSelect(coin: CoinCard, isSelect: Boolean) {
        val selectedCoins = state.value.selectedCoins.toMutableSet()
        if (isSelect) selectedCoins.add(coin) else selectedCoins.remove(coin)
        _state.update {
            it.copy(
                selectedCoins = selectedCoins
            )
        }
    }
}

sealed class CoinListEvent