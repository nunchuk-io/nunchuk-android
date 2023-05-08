package com.nunchuk.android.wallet.components.coin.detail.ancestry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.usecase.coin.GetCoinAncestryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoinAncestryViewModel @Inject constructor(
    private val getCoinAncestryUseCase: GetCoinAncestryUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val args: CoinAncestryFragmentArgs =
        CoinAncestryFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<CoinAncestryEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(CoinAncestryUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getCoinAncestryUseCase(GetCoinAncestryUseCase.Param(args.walletId, args.output.txid, args.output.vout)).onSuccess {
                val allCoins = it.toMutableList().apply {
                    add(0, listOf(args.output))
                }
                _state.update { state -> state.copy(coins = allCoins) }
            }
        }
    }
}

sealed class CoinAncestryEvent

data class CoinAncestryUiState(val coins: List<List<UnspentOutput>> = emptyList())