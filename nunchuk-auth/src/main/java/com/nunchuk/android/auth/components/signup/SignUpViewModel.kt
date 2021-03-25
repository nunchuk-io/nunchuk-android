package com.nunchuk.android.auth.components.signup

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NCViewModel
import com.nunchuk.android.auth.components.signup.SignUpEvent.*
import com.nunchuk.android.auth.domain.RegisterUseCase
import com.nunchuk.android.auth.validator.EmailValidator
import com.nunchuk.android.auth.validator.NameValidator
import com.nunchuk.android.auth.validator.doAfterValidate
import com.nunchuk.android.model.Result
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class SignUpViewModel @Inject constructor(
    private val nameValidator: NameValidator,
    private val emailValidator: EmailValidator,
    private val registerUseCase: RegisterUseCase
) : NCViewModel<Unit, SignUpEvent>() {

    override val initialState = Unit

    fun handleRegister(name: String, email: String) {
        val isNameValid = validateName(name)
        val isEmailValid = validateEmail(email)
        if (isNameValid && isEmailValid) {
            viewModelScope.launch {
                when (val result = registerUseCase.execute(name = name, email = email)) {
                    is Result.Success -> event(SignUpSuccessEvent)
                    is Result.Error -> event(SignUpErrorEvent(result.exception.message))
                    else -> event(LoadingEvent)
                }
            }
        }
    }

    fun validateEmail(email: String) = when {
        email.isBlank() -> doAfterValidate(false) { event(EmailRequiredEvent) }
        !emailValidator.valid(email) -> doAfterValidate(false) { event(EmailInvalidEvent) }
        else -> doAfterValidate { event(EmailValidEvent) }
    }

    fun validateName(name: String) = when {
        name.isBlank() -> doAfterValidate(false) { event(NameRequiredEvent) }
        !nameValidator.valid(name) -> doAfterValidate(false) { event(NameInvalidEvent) }
        else -> doAfterValidate { event(NameValidEvent) }
    }

}