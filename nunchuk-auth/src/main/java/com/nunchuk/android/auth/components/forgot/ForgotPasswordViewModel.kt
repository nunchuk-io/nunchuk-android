package com.nunchuk.android.auth.components.forgot

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.auth.domain.ForgotPasswordUseCase
import com.nunchuk.android.auth.validator.doAfterValidate
import com.nunchuk.android.model.Result
import com.nunchuk.android.utils.EmailValidator
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class ForgotPasswordViewModel @Inject constructor(
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
) : NunchukViewModel<Unit, ForgotPasswordEvent>() {

    override val initialState = Unit

    fun handleForgotPassword(email: String) {
        val isEmailValid = validateEmail(email)
        if (isEmailValid) {
            viewModelScope.launch {
                when (val result = forgotPasswordUseCase.execute(email = email)) {
                    is Result.Success -> event(ForgotPasswordEvent.ForgotPasswordSuccessEvent(email))
                    is Result.Error -> event(ForgotPasswordEvent.ForgotPasswordErrorEvent(result.exception.message))
                }
            }
        }
    }

    private fun validateEmail(email: String) = when {
        email.isBlank() -> doAfterValidate(false) { event(ForgotPasswordEvent.EmailRequiredEvent) }
        !EmailValidator.valid(email) -> doAfterValidate(false) { event(ForgotPasswordEvent.EmailInvalidEvent) }
        else -> doAfterValidate { event(ForgotPasswordEvent.EmailValidEvent) }
    }

}