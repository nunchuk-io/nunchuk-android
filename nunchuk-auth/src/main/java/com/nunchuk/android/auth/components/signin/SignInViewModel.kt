package com.nunchuk.android.auth.components.signin

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NCViewModel
import com.nunchuk.android.auth.components.signin.SignInEvent.*
import com.nunchuk.android.auth.domain.SignInUseCase
import com.nunchuk.android.auth.validator.EmailValidator
import com.nunchuk.android.auth.validator.doAfterValidate
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class SignInViewModel @Inject constructor(
    private val emailValidator: EmailValidator,
    private val signInUseCase: SignInUseCase
) : NCViewModel<Unit, SignInEvent>() {

    private var staySignIn = true

    override val initialState = Unit

    fun validateEmail(email: String) = when {
        email.isBlank() -> doAfterValidate(false) { event(EmailRequiredEvent) }
        !emailValidator.valid(email) -> doAfterValidate(false) { event(EmailInvalidEvent) }
        else -> doAfterValidate { event(EmailValidEvent) }
    }

    fun validatePassword(password: String) = when {
        password.isBlank() -> doAfterValidate(false) { event(PasswordRequiredEvent) }
        else -> doAfterValidate { event(PasswordValidEvent) }
    }

    fun handleSignIn(email: String, password: String) {
        val isEmailValid = validateEmail(email)
        val isPasswordValid = validatePassword(password)
        if (isEmailValid && isPasswordValid) {
            viewModelScope.launch {
                when (val result = signInUseCase.execute(email = email, password = password)) {
                    is Success -> event(SignInSuccessEvent(result.data.token.value))
                    is Error -> event(SignInErrorEvent(result.exception.message))
                }
            }
        }
    }

    fun storeStaySignIn(checked: Boolean) {
        staySignIn = checked
    }

}