package com.nunchuk.android.app.referral.confirmationcode

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.usecase.campaign.GetLocalReferrerCodeUseCase
import com.nunchuk.android.usecase.campaign.SendConfirmationCodeByEmailUseCase
import com.nunchuk.android.usecase.campaign.VerifyConfirmationCodeByEmailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReferralConfirmationCodeViewModel @Inject constructor(
    private val getLocalReferrerCodeUseCase: GetLocalReferrerCodeUseCase,
    private val sendConfirmationCodeByEmailUseCase: SendConfirmationCodeByEmailUseCase,
    private val verifyConfirmationCodeByEmailUseCase: VerifyConfirmationCodeByEmailUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val action: String = savedStateHandle["action"] ?: ""
    private val address: String? = savedStateHandle["address"]
        get() {
            return if (field == DEFAULT_ADDRESS) null else field
        }

    private val _state = MutableStateFlow(ReferralConfirmationCodeState())
    val state = _state.asStateFlow()

    private var requestedCode = false

    init {
        viewModelScope.launch {
            getLocalReferrerCodeUseCase(Unit)
                .collect { result ->
                    _state.update {
                        it.copy(
                            email = result.getOrNull()?.email.orEmpty(),
                        )
                    }
                    if (requestedCode.not()) {
                        requestedCode = true
                        sendConfirmationCodeByEmail()
                    }
                }
        }
    }

    fun sendConfirmationCodeByEmail() {
        val state = state.value
        if (state.email.isBlank() || action.isBlank()) return
        Log.e("referral", "address: $address email: ${state.email} action: $action")
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            sendConfirmationCodeByEmailUseCase(
                SendConfirmationCodeByEmailUseCase.Params(
                    receiveAddress = if (address == DEFAULT_ADDRESS) null else address,
                    email = state.email,
                    action = action
                )
            ).onSuccess { result ->
                _state.update {
                    it.copy(codeId = result)
                }
            }.onFailure { error ->
                _state.update { it.copy(errorMsg = error.message) }
            }
        }
        _state.update { it.copy(isLoading = false) }
    }

    fun verifyConfirmationCode(code: String) {
        val state = state.value
        if (state.codeId.isBlank() || state.email.isBlank()) return
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            verifyConfirmationCodeByEmailUseCase(
                VerifyConfirmationCodeByEmailUseCase.Params(
                    codeId = state.codeId,
                    code = code,
                    email = state.email
                )
            ).onSuccess { result ->
                _state.update {
                    it.copy(token = result)
                }
            }.onFailure { error ->
                _state.update { it.copy(errorMsg = error.message) }
            }
        }
        _state.update { it.copy(isLoading = false) }
    }

    fun onErrorMessageEventConsumed() {
        _state.update { state -> state.copy(errorMsg = null) }
    }

    fun onLoadingEventConsumed() {
        _state.update { state -> state.copy(isLoading = false) }
    }

    fun onTokenEventConsumed() {
        _state.update { state -> state.copy(token = null) }
    }
}

data class ReferralConfirmationCodeState(
    var email: String = "",
    var codeId: String = "",
    var errorMsg: String? = null,
    var isLoading: Boolean = false,
    val token: String? = null,
)