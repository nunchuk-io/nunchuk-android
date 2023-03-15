package com.nunchuk.android.wallet.components.coin.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.usecase.GetTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class CoinDetailViewModel @Inject constructor(
    getTransactionUseCase: GetTransactionUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val args: CoinDetailFragmentArgs =
        CoinDetailFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<CoinDetailEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(CoinDetailUiState())
    val state = _state.asStateFlow()

    init {
        getTransactionUseCase.execute(args.walletId, args.output.txid, false)
            .onEach { transition ->
                _state.update { it.copy(transaction = transition.transaction) }
            }
            .launchIn(viewModelScope)
    }
}

sealed class CoinDetailEvent