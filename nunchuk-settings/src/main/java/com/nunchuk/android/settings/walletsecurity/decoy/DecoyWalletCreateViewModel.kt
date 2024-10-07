package com.nunchuk.android.settings.walletsecurity.decoy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.usecase.CloneWalletUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DecoyWalletCreateViewModel @Inject constructor(
    private val cloneWalletUseCase: CloneWalletUseCase,
    private val getAssistedWalletsFlowUseCase: GetAssistedWalletsFlowUseCase,
    ) : ViewModel() {
    private val _state = MutableStateFlow(DecoyWalletCreateUiState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<DecoyWalletCreateEvent>()
    val event = _event.asSharedFlow()

    init {
        viewModelScope.launch {
            getAssistedWalletsFlowUseCase(Unit)
                .map { it.getOrElse { emptyList() } }
                .distinctUntilChanged()
                .collect { assistedWallets ->
                   _state.update {
                          it.copy(assistedWalletIds = assistedWallets.map { it.localId })
                   }
                }
        }
    }

    fun createDecoyWallet(walletId: String, decoyPin: String) {
        viewModelScope.launch {
            _event.emit(DecoyWalletCreateEvent.Loading(true))
            cloneWalletUseCase(
                CloneWalletUseCase.Params(
                    walletId = walletId,
                    decoyPin = decoyPin
                )
            ).onSuccess {
                _event.emit(DecoyWalletCreateEvent.WalletCreated)
            }.onFailure {
                _event.emit(DecoyWalletCreateEvent.Error(it.message.orEmpty()))
            }
            _event.emit(DecoyWalletCreateEvent.Loading(false))
        }
    }
}

sealed class DecoyWalletCreateEvent {
    data object WalletCreated : DecoyWalletCreateEvent()
    data class Error(val message: String) : DecoyWalletCreateEvent()
    data class Loading(val loading: Boolean) : DecoyWalletCreateEvent()
}

data class DecoyWalletCreateUiState(
    val assistedWalletIds: List<String> = emptyList()
)