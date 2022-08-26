package com.nunchuk.android.signer.software.components.primarykey.manuallyusername

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.CheckUsernamePrimaryKeyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PKeyManuallyUsernameViewModel @Inject constructor(private val checkUsernamePrimaryKeyUseCase: CheckUsernamePrimaryKeyUseCase) :
    NunchukViewModel<PKeyManuallyUsernameState, PKeyManuallyUsernameEvent>() {

    override val initialState: PKeyManuallyUsernameState = PKeyManuallyUsernameState()

    fun updateUsername(username: String) {
        updateState { copy(username = username) }
    }

    fun handleContinue() = viewModelScope.launch {
        val username = getState().username
        if (username.isBlank()) return@launch
        setEvent(PKeyManuallyUsernameEvent.LoadingEvent(true))
        val result = checkUsernamePrimaryKeyUseCase(CheckUsernamePrimaryKeyUseCase.Param(username = username))
        setEvent(PKeyManuallyUsernameEvent.LoadingEvent(false))
        if (result.isSuccess) {
            setEvent(PKeyManuallyUsernameEvent.CheckUsernameSuccess(username))
        } else {
            setEvent(PKeyManuallyUsernameEvent.ProcessFailure)
        }
    }

}