package com.nunchuk.android.main.membership.onchaintimelock.changetimelock

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.usecase.wallet.ChangeTimelockTypeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangeTimelockViewModel @Inject constructor(
    private val changeTimelockTypeUseCase: ChangeTimelockTypeUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(ChangeTimelockUiState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<ChangeTimelockEvent>()
    val event = _event.asSharedFlow()

    private val args: ChangeTimelockFragmentArgs =
        ChangeTimelockFragmentArgs.fromSavedStateHandle(savedStateHandle)

    init {
        _state.update { it.copy(
            walletId = args.walletId,
            groupId = args.groupId,
            slug = args.slug,
            walletType = args.walletType,
            isPersonal = args.isPersonal,
            setupPreference = args.setupPreference
        )}
    }

    fun onContinueClicked() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            changeTimelockTypeUseCase(
                ChangeTimelockTypeUseCase.Param(
                    groupId = args.groupId,
                    walletId = args.walletId
                )
            ).onSuccess {
                _state.update { it.copy(isLoading = false) }
                _event.emit(ChangeTimelockEvent.ChangeTimelockSuccess)
            }.onFailure { throwable ->
                _state.update { it.copy(isLoading = false) }
                _event.emit(ChangeTimelockEvent.ShowError(throwable.message.orUnknownError()))
            }
        }
    }
}

data class ChangeTimelockUiState(
    val walletId: String = "",
    val groupId: String? = null,
    val slug: String? = null,
    val walletType: String? = null,
    val isPersonal: Boolean = false,
    val setupPreference: String? = null,
    val isLoading: Boolean = false
)

sealed class ChangeTimelockEvent {
    object ChangeTimelockSuccess : ChangeTimelockEvent()
    data class ShowError(val message: String) : ChangeTimelockEvent()
}

