package com.nunchuk.android.auth.components.signin

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
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
) : NunchukViewModel<Unit, SignInEvent>() {

    private var staySignedIn = true

    override val initialState = Unit

    private fun validateEmail(email: String) = when {
        email.isBlank() -> doAfterValidate(false) { event(EmailRequiredEvent) }
        !emailValidator.valid(email) -> doAfterValidate(false) { event(EmailInvalidEvent) }
        else -> doAfterValidate { event(EmailValidEvent) }
    }

    private fun validatePassword(password: String) = when {
        password.isBlank() -> doAfterValidate(false) { event(PasswordRequiredEvent) }
        else -> doAfterValidate { event(PasswordValidEvent) }
    }

    fun handleSignIn(email: String, password: String) {
        val isEmailValid = validateEmail(email)
        val isPasswordValid = validatePassword(password)
        if (isEmailValid && isPasswordValid) {
            viewModelScope.launch {
                when (val result = signInUseCase.execute(email = email, password = password, staySignedIn = staySignedIn)) {
                    is Success -> event(SignInSuccessEvent(result.data.tokenId))
                    is Error -> event(SignInErrorEvent(result.exception.message))
                }
            }
        }
    }

    fun storeStaySignedIn(staySignedIn: Boolean) {
        this.staySignedIn = staySignedIn
    }

}