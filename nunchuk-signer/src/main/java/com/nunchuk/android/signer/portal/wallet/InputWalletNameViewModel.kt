package com.nunchuk.android.signer.portal.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.usecase.UpdateWalletUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InputWalletNameViewModel @Inject constructor(
    private val updateWalletUseCase: UpdateWalletUseCase,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(InputWalletNameUiState())
    val state = _state.asStateFlow()

    fun updateWalletName(walletId: String, walletName: String) {
        viewModelScope.launch {
            getWalletDetail2UseCase(walletId)
                .onSuccess { wallet ->
                    updateWalletUseCase(
                        UpdateWalletUseCase.Params(
                            wallet.copy(name = walletName)
                        )
                    )
                    _state.update { it.copy(isUpdateNameSuccess = true) }
                }
        }
    }

    fun markUpdateNameSuccess() {
        _state.update { it.copy(isUpdateNameSuccess = false) }
    }
}

data class InputWalletNameUiState(
    val isUpdateNameSuccess: Boolean = false,
)