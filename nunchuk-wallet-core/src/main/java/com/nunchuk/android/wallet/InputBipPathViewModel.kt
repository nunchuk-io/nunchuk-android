package com.nunchuk.android.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.usecase.IsValidDerivationPathUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InputBipPathViewModel @Inject constructor(
    private val isValidDerivationPathUseCase: IsValidDerivationPathUseCase
) : ViewModel() {
    private val _event = MutableSharedFlow<InputBipPathEvent>()
    val event = _event.asSharedFlow()

    fun checkBipPath(path: String) {
        viewModelScope.launch {
            val result = isValidDerivationPathUseCase(path)
            _event.emit(InputBipPathEvent.OnVerifyPath(result.isSuccess && result.getOrThrow()))
        }
    }
}

sealed class InputBipPathEvent {
    data class Loading(val isLoading: Boolean) : InputBipPathEvent()
    data class OnVerifyPath(val isValid: Boolean) : InputBipPathEvent()
}