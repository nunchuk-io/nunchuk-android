package com.nunchuk.android.main.membership.byzantine.select

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.main.membership.model.GroupWalletType
import com.nunchuk.android.usecase.membership.GetGroupAssistedWalletConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectGroupViewModel @Inject constructor(
    private val getGroupAssistedWalletConfigUseCase: GetGroupAssistedWalletConfigUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(SelectGroupUiState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<SelectGroupEvent>()
    val event = _event.asSharedFlow()

    init {
        viewModelScope.launch {
            _event.emit(SelectGroupEvent.Loading(true))
            getGroupAssistedWalletConfigUseCase(Unit)
                .onSuccess { config ->
                    _state.update {
                        it.copy(
                            remainingByzantineWallet = config.remainingByzantineWallet,
                            remainingByzantineProWallet = config.remainingByzantineProWallet
                        )
                    }
                }
            _event.emit(SelectGroupEvent.Loading(false))
        }
    }

    fun checkGroupTypeAvailable(groupWalletType: GroupWalletType): Boolean {
        return (groupWalletType.isPro && state.value.remainingByzantineProWallet > 0) || (groupWalletType.isPro.not() && state.value.remainingByzantineWallet > 0)
    }
}

sealed class SelectGroupEvent {
    data class Loading(val isLoading: Boolean) : SelectGroupEvent()
}

data class SelectGroupUiState(
    val remainingByzantineWallet: Int = 0,
    val remainingByzantineProWallet: Int = 0
)