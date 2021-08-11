package com.nunchuk.android.auth.components.signin

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.auth.components.signin.SignInEvent.*
import com.nunchuk.android.auth.domain.GetCurrentUserUseCase
import com.nunchuk.android.auth.domain.LoginWithMatrixUseCase
import com.nunchuk.android.auth.domain.SignInUseCase
import com.nunchuk.android.auth.validator.doAfterValidate
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.usecase.InitNunchukUseCase
import com.nunchuk.android.utils.EmailValidator
import kotlinx.coroutines.flow.*
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

internal class SignInViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val loginWithMatrixUseCase: LoginWithMatrixUseCase,
    private val initNunchukUseCase: InitNunchukUseCase,
    private val accountManager: AccountManager
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
            signInUseCase.execute(email = email, password = password, staySignedIn = staySignedIn)
                .onStart { event(ProcessingEvent) }
                .catch { event(SignInErrorEvent(it.message)) }
                .flatMapConcat { getCurrentUser(it) }
                .onEach { event(SignInSuccessEvent) }
                .launchIn(viewModelScope)
        }
    }

    private fun getCurrentUser(token: String): Flow<Unit> {
        return getCurrentUserUseCase.execute()
            .flatMapConcat { loginWithMatrix(it, token) }
            .flatMapConcat { initNunchuk() }
    }

    private fun initNunchuk(): Flow<Unit> {
        val account = accountManager.getAccount()
        return initNunchukUseCase.execute(account.email, account.chatId)
    }

    private fun loginWithMatrix(userName: String, password: String): Flow<Session> {
        return loginWithMatrixUseCase.execute(userName, password)
            .onEach {
                SessionHolder.currentSession = it.apply {
                    open()
                    startSync(true)
                }
            }
    }

    fun storeStaySignedIn(staySignedIn: Boolean) {
        this.staySignedIn = staySignedIn
    }

}