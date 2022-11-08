package com.nunchuk.android.main.components.tabs.services.keyrecovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.membership.VerifiedPasswordTargetAction
import com.nunchuk.android.core.domain.membership.VerifiedPasswordTokenUseCase
import com.nunchuk.android.core.util.orUnknownError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KeyRecoveryViewModel @Inject constructor(
    private val verifiedPasswordTokenUseCase: VerifiedPasswordTokenUseCase
) :
    ViewModel() {

    private val _event = MutableSharedFlow<KeyRecoveryEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(KeyRecoveryState())
    val state = _state.asStateFlow()

    fun onItemClick(item: KeyRecoveryActionItem) = viewModelScope.launch {
        _event.emit(KeyRecoveryEvent.ItemClick(item))
    }

    fun confirmPassword(password: String, item: KeyRecoveryActionItem) = viewModelScope.launch {
        if (password.isBlank()) {
            return@launch
        }
        _event.emit(KeyRecoveryEvent.Loading(true))
        val targetAction = when (item) {
            is KeyRecoveryActionItem.StartKeyRecovery -> {
                VerifiedPasswordTargetAction.DOWNLOAD_KEY_BACKUP.name
            }
            is KeyRecoveryActionItem.UpdateRecoveryQuestion -> {
                VerifiedPasswordTargetAction.UPDATE_SECURITY_QUESTIONS.name
            }
        }
        val result = verifiedPasswordTokenUseCase(
            VerifiedPasswordTokenUseCase.Param(
                targetAction = targetAction,
                password = password
            )
        )
        _event.emit(KeyRecoveryEvent.Loading(false))
        if (result.isSuccess) {
            _event.emit(KeyRecoveryEvent.CheckPasswordSuccess(item, result.getOrThrow().orEmpty()))
        } else {
            _event.emit(KeyRecoveryEvent.ProcessFailure(message = result.exceptionOrNull()?.message.orUnknownError()))
        }
    }
}