package com.nunchuk.android.auth.components.enterxpub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.auth.util.orUnknownError
import com.nunchuk.android.usecase.GetSignInDummyTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EnterXPUBViewModel @Inject constructor(
    val getSigninDummyTransactionUseCase: GetSignInDummyTransactionUseCase
) : ViewModel() {

    private val _event = MutableSharedFlow<EnterXPUBEvent>()
    val event = _event.asSharedFlow()

    fun signinDummy(data: String) {
        viewModelScope.launch {
            _event.emit(EnterXPUBEvent.Loading(true))
            getSigninDummyTransactionUseCase(
                GetSignInDummyTransactionUseCase.Param(
                    data = data
                )
            ).onSuccess {
                _event.emit(EnterXPUBEvent.Success(
                    requiredSignatures = it.requiredSignatures,
                    dummyTransactionId = it.dummyTransactionId,
                    signInData = data
                ))
            }.onFailure {
                _event.emit(EnterXPUBEvent.Error(it.message.orUnknownError()))
            }
            _event.emit(EnterXPUBEvent.Loading(false))
        }
    }
}

sealed class EnterXPUBEvent {
    data class Loading(val loading: Boolean) : EnterXPUBEvent()
    data class Success(
        val requiredSignatures: Int,
        val dummyTransactionId: String,
        val signInData: String
    ) : EnterXPUBEvent()
    data class Error(val message: String) : EnterXPUBEvent()
}