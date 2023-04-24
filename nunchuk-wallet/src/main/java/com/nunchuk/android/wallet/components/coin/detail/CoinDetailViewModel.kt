package com.nunchuk.android.wallet.components.coin.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.usecase.GetTransactionUseCase
import com.nunchuk.android.usecase.coin.LockCoinUseCase
import com.nunchuk.android.usecase.coin.UnLockCoinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoinDetailViewModel @Inject constructor(
    private val getTransactionUseCase: GetTransactionUseCase,
    private val lockCoinUseCase: LockCoinUseCase,
    private val unLockCoinUseCase: UnLockCoinUseCase,
    private val assistedWalletManager: AssistedWalletManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val args: CoinDetailFragmentArgs =
        CoinDetailFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<CoinDetailEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(CoinDetailUiState())
    val state = _state.asStateFlow()

    init {
        getTransactionDetail()
    }

    fun getTransactionDetail() {
        getTransactionUseCase.execute(args.walletId, args.txId, false)
            .onEach { transition ->
                _state.update { it.copy(transaction = transition.transaction) }
            }
            .launchIn(viewModelScope)
    }

    fun lockCoin(isLocked: Boolean) {
        viewModelScope.launch {
            val result = if (isLocked) lockCoinUseCase(
                LockCoinUseCase.Params(
                    args.walletId,
                    args.txId,
                    args.vout,
                    assistedWalletManager.isActiveAssistedWallet(args.walletId)
                )
            )
            else unLockCoinUseCase(
                UnLockCoinUseCase.Params(
                    args.walletId,
                    args.txId,
                    args.vout,
                    assistedWalletManager.isActiveAssistedWallet(args.walletId)
                )
            )
            result.onSuccess {
                _event.emit(CoinDetailEvent.LockOrUnlockSuccess(isLocked))
            }.onFailure {
                _event.emit(CoinDetailEvent.ShowError(it.message.orEmpty()))
            }
        }
    }
}

sealed class CoinDetailEvent {
    data class ShowError(val message: String) : CoinDetailEvent()
    data class LockOrUnlockSuccess(val isLocked: Boolean) : CoinDetailEvent()
}