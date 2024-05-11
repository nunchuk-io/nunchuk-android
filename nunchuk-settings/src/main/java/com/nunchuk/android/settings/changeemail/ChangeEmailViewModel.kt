package com.nunchuk.android.settings.changeemail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.nunchuk.android.core.domain.ClearInfoSessionUseCase
import com.nunchuk.android.core.domain.membership.CalculateRequiredSignaturesChangeEmailUseCase
import com.nunchuk.android.core.domain.membership.ChangeEmailUseCase
import com.nunchuk.android.core.profile.SendSignOutUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.VerificationType
import com.nunchuk.android.settings.AccountSettingEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangeEmailViewModel @Inject constructor(
    private val calculateRequiredSignaturesChangeEmailUseCase: CalculateRequiredSignaturesChangeEmailUseCase,
    private val changeEmailUseCase: ChangeEmailUseCase,
    private val gson: Gson,
    private val sendSignOutUseCase: SendSignOutUseCase,
    private val clearInfoSessionUseCase: ClearInfoSessionUseCase,
    private val appScope: CoroutineScope,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val args = ChangeEmailFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<ChangeEmailEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(ChangeEmailState())
    val state = _state.asStateFlow()

    fun updateEmail(email: String) {
        viewModelScope.launch {
            _state.update { it.copy(email = email) }
        }
    }

    fun calculateRequiredSignatures(confirmCodeNonce: String, confirmCodeToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(confirmCodeNonce = confirmCodeNonce, confirmCodeToken = confirmCodeToken) }
            _event.emit(ChangeEmailEvent.Loading(true))
            val resultCalculate = calculateRequiredSignaturesChangeEmailUseCase(
                CalculateRequiredSignaturesChangeEmailUseCase.Param(newEmail = state.value.email)
            )
            _event.emit(ChangeEmailEvent.Loading(false))
            if (resultCalculate.isSuccess) {
                val signatures = resultCalculate.getOrThrow()
                if (signatures.type == VerificationType.SIGN_DUMMY_TX) {
                    _event.emit(ChangeEmailEvent.Loading(true))
                    changeEmailUseCase(
                        ChangeEmailUseCase.Param(
                            verifyToken = args.verifyToken,
                            securityQuestionToken = "",
                            confirmCodeNonce = confirmCodeNonce,
                            confirmCodeToken = confirmCodeToken,
                            newEmail = state.value.email,
                            draft = true
                        )
                    ).onSuccess { dummyTransactionPayload ->
                        _event.emit(
                            ChangeEmailEvent.CalculateRequiredSignaturesSuccess(
                                userData = state.value.email,
                                requiredSignatures = resultCalculate.getOrThrow().requiredSignatures,
                                type = resultCalculate.getOrThrow().type,
                                dummyTransactionId = dummyTransactionPayload?.dummyTransactionId.orEmpty(),
                                newEmail = state.value.email,
                                walletId = dummyTransactionPayload?.walletId.orEmpty(),
                                groupId = dummyTransactionPayload?.groupId.orEmpty()
                            )
                        )
                    }.onFailure {
                        _event.emit(ChangeEmailEvent.Error(it.message.orUnknownError()))
                    }
                    _event.emit(ChangeEmailEvent.Loading(false))
                } else if (signatures.type == VerificationType.SECURITY_QUESTION) {
                    _event.emit(
                        ChangeEmailEvent.CalculateRequiredSignaturesSuccess(
                            userData = state.value.email,
                            requiredSignatures = resultCalculate.getOrThrow().requiredSignatures,
                            type = resultCalculate.getOrThrow().type,
                            dummyTransactionId = "",
                            newEmail = state.value.email,
                        )
                    )
                } else {
                    changeEmail("")
                }
            } else {
                _event.emit(ChangeEmailEvent.Error(resultCalculate.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun changeEmail(
        securityQuestionToken: String
    ) {
        viewModelScope.launch {
            _event.emit(ChangeEmailEvent.Loading(true))
            changeEmailUseCase(
                ChangeEmailUseCase.Param(
                    verifyToken = args.verifyToken,
                    securityQuestionToken = securityQuestionToken,
                    confirmCodeNonce = _state.value.confirmCodeNonce,
                    confirmCodeToken = _state.value.confirmCodeToken,
                    newEmail = state.value.email,
                    draft = false
                )
            ).onSuccess {
                handleSignOutEvent()
            }.onFailure {
                _event.emit(ChangeEmailEvent.Error(it.message.orUnknownError()))
            }
        }
    }

    fun getUserData(): String {
        val emailMap = mapOf("new_email" to state.value.email)
        return gson.toJson(emailMap)
    }

    fun handleSignOutEvent() {
        appScope.launch {
            _event.emit(ChangeEmailEvent.Loading(true))
            clearInfoSessionUseCase.invoke(Unit)
            sendSignOutUseCase(Unit)
            _event.emit(ChangeEmailEvent.SignOutEvent)
        }
    }
}

data class ChangeEmailState(
    val code: String = "",
    val email: String = "",
    val action: String = "",
    val confirmCodeNonce: String = "",
    val confirmCodeToken: String = "",
)

sealed class ChangeEmailEvent {
    data class Error(val message: String) : ChangeEmailEvent()
    data class Loading(val loading: Boolean) : ChangeEmailEvent()
    data class CalculateRequiredSignaturesSuccess(
        val userData: String,
        val requiredSignatures: Int,
        val type: String,
        val dummyTransactionId: String? = null,
        val newEmail: String,
        val walletId: String = "",
        val groupId: String = "",
    ) : ChangeEmailEvent()

    data object SignOutEvent : ChangeEmailEvent()
}