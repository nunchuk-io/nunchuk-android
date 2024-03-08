package com.nunchuk.android.app.onboard.unassisted

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.usecase.wallet.CreateHotWalletUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UnAssistedIntroViewModel @Inject constructor(
    private val createHotWalletUseCase: CreateHotWalletUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(UnAssistedIntroState())
    val state = _state.asStateFlow()

    fun createHotWallet() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            createHotWalletUseCase(Unit)
                .onSuccess {
                    _state.update { it.copy(openMainScreen = true) }
                }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun handledOpenMainScreen() {
        _state.update { it.copy(openMainScreen = false) }
    }
}

data class UnAssistedIntroState(
    val isLoading: Boolean = false,
    val openMainScreen: Boolean = false,
)