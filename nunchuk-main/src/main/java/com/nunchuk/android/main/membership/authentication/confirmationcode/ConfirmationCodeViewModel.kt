package com.nunchuk.android.main.membership.authentication.confirmationcode

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.main.membership.authentication.WalletAuthenticationActivityArgs
import com.nunchuk.android.usecase.membership.RequestConfirmationCodeUseCase
import com.nunchuk.android.usecase.membership.VerifyConfirmationCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfirmationCodeViewModel @Inject constructor(
    accountManager: AccountManager,
    savedStateHandle: SavedStateHandle,
    private val verifyConfirmationCodeUseCase: VerifyConfirmationCodeUseCase,
    private val requestConfirmationCodeUseCase: RequestConfirmationCodeUseCase
) : ViewModel() {

    private val args = WalletAuthenticationActivityArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<ConfirmChangeEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(ConfirmChangeState())
    val state = _state.asStateFlow()

    var email = accountManager.getAccount().email
    private var codeId: String? = null
    private var nonce: String? = null

    init {
        if (args.newEmail.isNullOrEmpty().not()) {
            email = args.newEmail.orEmpty()
        }
        viewModelScope.launch {
            if (args.action.isNullOrEmpty()) return@launch
            _event.emit(ConfirmChangeEvent.Loading(true))
            val result = requestConfirmationCodeUseCase(
                RequestConfirmationCodeUseCase.Param(
                    action = args.action.orEmpty(),
                    userData = args.userData
                )
            )
            _event.emit(ConfirmChangeEvent.Loading(false))
            if (result.isSuccess) {
                nonce = result.getOrThrow().first
                codeId = result.getOrThrow().second
            } else {
                _event.emit(ConfirmChangeEvent.RequestCodeError)
            }
        }
    }

    fun getAction() = args.action.orEmpty()

    fun onCodeChange(code: String) {
        _state.update {
            it.copy(code = code)
        }
    }

    fun verifyCode() = viewModelScope.launch {
        if (codeId.isNullOrEmpty()) return@launch
        _event.emit(ConfirmChangeEvent.Loading(true))
        val result = verifyConfirmationCodeUseCase(
            VerifyConfirmationCodeUseCase.Param(
                code = _state.value.code,
                codeId = codeId!!
            )
        )
        _event.emit(ConfirmChangeEvent.Loading(false))
        if (result.isSuccess) {
            _event.emit(ConfirmChangeEvent.VerifyCodeSuccess(result.getOrThrow(), nonce!!))
        } else {
            _event.emit(ConfirmChangeEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }
}