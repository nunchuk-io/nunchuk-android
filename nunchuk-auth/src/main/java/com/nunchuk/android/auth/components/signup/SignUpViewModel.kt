package com.nunchuk.android.auth.components.signup

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.auth.components.signup.SignUpEvent.*
import com.nunchuk.android.auth.domain.RegisterUseCase
import com.nunchuk.android.auth.validator.NameValidator
import com.nunchuk.android.auth.validator.doAfterValidate
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.network.NunchukApiException
import com.nunchuk.android.network.accountExisted
import com.nunchuk.android.utils.EmailValidator
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class SignUpViewModel @Inject constructor(
    private val nameValidator: NameValidator,
    private val registerUseCase: RegisterUseCase
) : NunchukViewModel<Unit, SignUpEvent>() {

    override val initialState = Unit

    fun handleRegister(name: String, email: String) {
        val isNameValid = validateName(name)
        val isEmailValid = validateEmail(email)
        if (isNameValid && isEmailValid) {
            viewModelScope.launch {
                event(LoadingEvent)
                when (val result = registerUseCase.execute(name = name, email = email)) {
                    is Success -> event(SignUpSuccessEvent)
                    is Error -> handleException(result)
                }
            }
        }
    }

    private fun handleException(result: Error) {
        val exception = result.exception
        if (exception is NunchukApiException && exception.accountExisted()) {
            event(AccountExistedEvent(exception.message))
        } else {
            event(SignUpErrorEvent(exception.message))
        }
    }

    private fun validateEmail(email: String) = when {
        email.isBlank() -> doAfterValidate(false) { event(EmailRequiredEvent) }
        !EmailValidator.valid(email) -> doAfterValidate(false) { event(EmailInvalidEvent) }
        else -> doAfterValidate { event(EmailValidEvent) }
    }

    private fun validateName(name: String) = when {
        name.isBlank() -> doAfterValidate(false) { event(NameRequiredEvent) }
        !nameValidator.valid(name) -> doAfterValidate(false) { event(NameInvalidEvent) }
        else -> doAfterValidate { event(NameValidEvent) }
    }

}