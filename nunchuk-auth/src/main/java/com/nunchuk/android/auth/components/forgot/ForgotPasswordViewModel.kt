package com.nunchuk.android.auth.components.forgot

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.auth.components.forgot.ForgotPasswordEvent.*
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
        if (validateEmail(email)) {
            viewModelScope.launch {
                event(LoadingEvent)
                when (val result = forgotPasswordUseCase.execute(email = email)) {
                    is Result.Success -> event(ForgotPasswordSuccessEvent(email))
                    is Result.Error -> event(ForgotPasswordErrorEvent(result.exception.message))
                }
            }
        }
    }

    private fun validateEmail(email: String) = when {
        email.isBlank() -> doAfterValidate(false) { event(EmailRequiredEvent) }
        !EmailValidator.valid(email) -> doAfterValidate(false) { event(EmailInvalidEvent) }
        else -> doAfterValidate { event(EmailValidEvent) }
    }

}