package com.nunchuk.android.auth.components.signin

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.auth.components.signin.SignInEvent.*
import com.nunchuk.android.auth.domain.GetCurrentUserUseCase
import com.nunchuk.android.auth.domain.LoginWithMatrixUseCase
import com.nunchuk.android.auth.domain.SignInUseCase
import com.nunchuk.android.auth.validator.doAfterValidate
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.process
import com.nunchuk.android.utils.EmailValidator
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class SignInViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val loginWithMatrixUseCase: LoginWithMatrixUseCase
) : NunchukViewModel<Unit, SignInEvent>() {

    private var staySignedIn = true

    override val initialState = Unit

    private fun validateEmail(email: String) = when {
        email.isBlank() -> doAfterValidate(false) { event(EmailRequiredEvent) }
        !EmailValidator.valid(email) -> doAfterValidate(false) { event(EmailInvalidEvent) }
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
            event(ProcessingEvent)
            process({
                signInUseCase.execute(email = email, password = password, staySignedIn = staySignedIn)
            }, ::getCurrentUser, {
                event(SignInErrorEvent(it.message))
            })
        }
    }

    private fun getCurrentUser(token: String) {
        process(getCurrentUserUseCase::execute, { loginWithMatrix(it, token) }, {
            event(SignInErrorEvent(it.message))
        })
    }

    private fun loginWithMatrix(userName: String, password: String) {
        viewModelScope.launch {
            loginWithMatrixUseCase.execute(userName, password)
                .catch {
                    event(SignInErrorEvent(it.message))
                }
                .collect {
                    event(SignInSuccessEvent)
                    SessionHolder.currentSession = it.apply {
                        open()
                        startSync(true)
                    }
                }
        }
    }

    fun storeStaySignedIn(staySignedIn: Boolean) {
        this.staySignedIn = staySignedIn
    }

}