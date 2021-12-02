package com.nunchuk.android.auth.components.signin

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.auth.components.signin.SignInEvent.*
import com.nunchuk.android.auth.domain.GetCurrentUserUseCase
import com.nunchuk.android.auth.domain.LoginWithMatrixUseCase
import com.nunchuk.android.auth.domain.SignInUseCase
import com.nunchuk.android.auth.util.orUnknownError
import com.nunchuk.android.auth.validator.doAfterValidate
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.network.NunchukApiException
import com.nunchuk.android.share.InitNunchukUseCase
import com.nunchuk.android.utils.EmailValidator
import com.nunchuk.android.utils.onException
import kotlinx.coroutines.Dispatchers
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

    private var staySignedIn = false

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
                .flowOn(Dispatchers.IO)
                .onStart { event(ProcessingEvent) }
                .onException {
                    if (it is NunchukApiException) {
                        event(
                            SignInErrorEvent(
                                code = it.code,
                                message = it.message,
                                errorDetail = it.errorDetail
                            )
                        )
                    } else {
                        event(SignInErrorEvent(message = it.message.orUnknownError()))
                    }
                }
                .flatMapConcat { getCurrentUser(token = it.first, encryptedDeviceId = it.second) }
                .onEach { event(SignInSuccessEvent) }
                .flowOn(Dispatchers.Main)
                .launchIn(viewModelScope)
        }
    }

    private fun getCurrentUser(token: String, encryptedDeviceId: String): Flow<Unit> {
        return getCurrentUserUseCase.execute()
            .flatMapConcat { loginWithMatrix(userName = it, password = token, encryptedDeviceId = encryptedDeviceId) }
            .flatMapConcat { initNunchuk() }
    }

    private fun initNunchuk(): Flow<Unit> {
        val account = accountManager.getAccount()
        return initNunchukUseCase.execute(accountId = account.email)
            .onException { event(SignInErrorEvent(message = it.message)) }
    }

    private fun loginWithMatrix(userName: String, password: String, encryptedDeviceId: String): Flow<Session> {
        return loginWithMatrixUseCase.execute(userName = userName, password = password, encryptedDeviceId = encryptedDeviceId)
            .onException { event(SignInErrorEvent(message = it.message)) }
            .onEach {
                SessionHolder.storeActiveSession(it)
            }
    }

    fun storeStaySignedIn(staySignedIn: Boolean) {
        this.staySignedIn = staySignedIn
    }

}