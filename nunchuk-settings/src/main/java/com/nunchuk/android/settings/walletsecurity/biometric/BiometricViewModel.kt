package com.nunchuk.android.settings.walletsecurity.biometric

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.ClearInfoSessionUseCase
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.profile.SendSignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BiometricViewModel @Inject constructor(
    private val clearInfoSessionUseCase: ClearInfoSessionUseCase,
    private val sendSignOutUseCase: SendSignOutUseCase,
    private val signInModeHolder: SignInModeHolder,
) : ViewModel() {

    private val _event = MutableSharedFlow<BiometricEvent>()
    val event = _event.asSharedFlow()

    fun signOut() {
        viewModelScope.launch {
            signInModeHolder.clear()
            clearInfoSessionUseCase.invoke(Unit)
            sendSignOutUseCase(Unit)
            _event.emit(BiometricEvent.SignOut)
        }
    }
}

sealed class BiometricEvent {
    data object SignOut : BiometricEvent()
}