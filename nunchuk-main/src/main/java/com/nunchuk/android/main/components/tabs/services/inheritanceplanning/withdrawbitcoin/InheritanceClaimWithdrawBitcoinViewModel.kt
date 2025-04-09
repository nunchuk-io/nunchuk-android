package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.withdrawbitcoin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.usecase.GetWalletsUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InheritanceClaimWithdrawBitcoinViewModel @Inject constructor(
    private val getWalletsUseCase: GetWalletsUseCase,
) : ViewModel() {

    private val _event = MutableSharedFlow<InheritanceClaimWithdrawBitcoinEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(InheritanceClaimWithdrawBitcoinState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getWalletsUseCase.execute()
                .onException { }
                .flowOn(Dispatchers.Main)
                .collect { wallets ->
                    if (_state.value.oldWalletIds == null) {
                        _state.update {
                            it.copy(oldWalletIds = wallets.map { wallet -> wallet.wallet.id })
                        }
                    }
                }
        }
    }

    fun checkNewWallets() = viewModelScope.launch {
        getWalletsUseCase.execute()
            .onException { _event.emit(InheritanceClaimWithdrawBitcoinEvent.Error(it.message.orUnknownError())) }
            .flowOn(Dispatchers.Main)
            .collect { wallets ->
                val newWallets = wallets.filter { wallet -> wallet.wallet.id !in state.value.oldWalletIds.orEmpty() }
                _event.emit(InheritanceClaimWithdrawBitcoinEvent.CheckNewWallet(newWallets.isNotEmpty()))
            }
    }

    fun checkWallet() = viewModelScope.launch {
        val hasWallet = _state.value.oldWalletIds.isNullOrEmpty().not()
        _event.emit(InheritanceClaimWithdrawBitcoinEvent.CheckHasWallet(hasWallet))
    }
}

data class InheritanceClaimWithdrawBitcoinState(
    val oldWalletIds: List<String>? = null,
)

sealed class InheritanceClaimWithdrawBitcoinEvent {
    data class Loading(val isLoading: Boolean) : InheritanceClaimWithdrawBitcoinEvent()
    data class Error(val message: String) : InheritanceClaimWithdrawBitcoinEvent()
    data class CheckHasWallet(val isHasWallet: Boolean) : InheritanceClaimWithdrawBitcoinEvent()
    data class CheckNewWallet(val isNewWallet: Boolean) : InheritanceClaimWithdrawBitcoinEvent()
}