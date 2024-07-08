package com.nunchuk.android.main.rollover.transferfund

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.usecase.coin.GetAllCollectionsUseCase
import com.nunchuk.android.usecase.coin.GetAllTagsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RollOverTransferFundViewModel @Inject constructor(
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val getAllCollectionsUseCase: GetAllCollectionsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args = RollOverTransferFundFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _uiState = MutableStateFlow(RollOverTransferFundUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getAllTagsUseCase(args.oldWalletId).onSuccess { tags ->
                _uiState.update { it.copy(isHasTagOrCollection = tags.isNotEmpty()) }
            }
        }
        viewModelScope.launch {
            getAllCollectionsUseCase(args.oldWalletId).onSuccess { collections ->
                _uiState.update { it.copy(isHasTagOrCollection = collections.isNotEmpty()) }
            }
        }
    }

    fun isHasTagOrCollection(): Boolean {
        return uiState.value.isHasTagOrCollection
    }

    fun updateWallets(oldWallet: Wallet, newWallet: Wallet) {
        _uiState.update { it.copy(oldWallet = oldWallet, newWallet = newWallet) }
    }
}

data class RollOverTransferFundUiState(
    val isHasTagOrCollection: Boolean = false,
    val oldWallet: Wallet = Wallet(),
    val newWallet: Wallet = Wallet()
)