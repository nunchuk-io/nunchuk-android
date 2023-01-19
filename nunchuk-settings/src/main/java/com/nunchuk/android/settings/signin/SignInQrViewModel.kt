package com.nunchuk.android.settings.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.auth.domain.ConfirmQrSignInUseCase
import com.nunchuk.android.auth.domain.TryQrSignInUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.setting.QrSignInData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInQrViewModel @Inject constructor(
    private val tryQrSignInUseCase: TryQrSignInUseCase,
    private val confirmQrSignInUseCase: ConfirmQrSignInUseCase,
) : ViewModel() {
    private val _event = MutableSharedFlow<SignInQrEvent>()
    val event = _event.asSharedFlow()

    var acceptQr: Boolean = true

    fun trySignIn(qr: String) {
        if (acceptQr.not()) return
        viewModelScope.launch {
            acceptQr = false
            _event.emit(SignInQrEvent.Loading(true))
            val result = tryQrSignInUseCase(qr)
            _event.emit(SignInQrEvent.Loading(false))
            if (result.isSuccess) {
                _event.emit(SignInQrEvent.TrySignInSuccess(result.getOrThrow()))
            } else {
                acceptQr = true
                _event.emit(SignInQrEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun enableAcceptQr() {
        acceptQr = true
    }

    fun confirmSignIn(data: QrSignInData) {
        viewModelScope.launch {
            _event.emit(SignInQrEvent.Loading(true))
            val result = confirmQrSignInUseCase(data)
            _event.emit(SignInQrEvent.Loading(false))
            if (result.isSuccess) {
                _event.emit(SignInQrEvent.ConfirmSignInSuccess)
            } else {
                _event.emit(SignInQrEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }
}

sealed class SignInQrEvent {
    object ConfirmSignInSuccess : SignInQrEvent()
    data class Loading(val isLoading: Boolean) : SignInQrEvent()
    data class TrySignInSuccess(val data: QrSignInData) : SignInQrEvent()
    data class ShowError(val error: String) : SignInQrEvent()
}