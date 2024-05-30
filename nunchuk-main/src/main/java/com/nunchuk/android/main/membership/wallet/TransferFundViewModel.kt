package com.nunchuk.android.main.membership.wallet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.usecase.wallet.GetUnusedWalletAddressUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransferFundViewModel @Inject constructor(
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val getUnusedWalletAddressUseCase: GetUnusedWalletAddressUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args = TransferFundFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _uiState = MutableStateFlow(TransferFundUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getUnusedWalletAddressUseCase(args.walletId).onSuccess { addresses ->
                _uiState.update { state ->
                    state.copy(address = addresses.first())
                }
            }
        }
        viewModelScope.launch {
            getWalletDetail2UseCase(args.replacedWalletId).onSuccess { wallet ->
                _uiState.update { it.copy(replacedWallet = wallet) }
            }
        }
        viewModelScope.launch {
            getWalletDetail2UseCase(args.walletId).onSuccess { wallet ->
                _uiState.update { it.copy(newWallet = wallet) }
            }
        }
    }
}

data class TransferFundUiState(
    val replacedWallet: Wallet = Wallet(),
    val newWallet: Wallet = Wallet(),
    val address: String = "",
)