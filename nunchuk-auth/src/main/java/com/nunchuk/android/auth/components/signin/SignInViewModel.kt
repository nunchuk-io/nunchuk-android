package com.nunchuk.android.auth.components.signin

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.auth.components.signin.SignInEvent.*
import com.nunchuk.android.auth.domain.SignInUseCase
import com.nunchuk.android.auth.util.orUnknownError
import com.nunchuk.android.auth.validator.doAfterValidate
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.network.NunchukApiException
import com.nunchuk.android.share.InitNunchukUseCase
import com.nunchuk.android.utils.EmailValidator
import com.nunchuk.android.utils.onException
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.*
import javax.inject.Inject

internal class SignInViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val initNunchukUseCase: InitNunchukUseCase,
    private val accountManager: AccountManager
) : NunchukViewModel<Unit, SignInEvent>() {

    private var staySignedIn = false

    override val initialState = Unit

    private var token: String? = null
    private var encryptedDeviceId: String? = null

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
        if (validateEmail(email) && validatePassword(password)) {
            signInUseCase.execute(email = email, password = password, staySignedIn = staySignedIn)
                .onStart { event(ProcessingEvent) }
                .flowOn(IO)
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
                .flatMapConcat {
                    token = it.first
                    encryptedDeviceId = it.second
                    initNunchuk()
                }
                .onEach {
                    event(
                        SignInSuccessEvent(
                            token = token.orEmpty(),
                            deviceId = encryptedDeviceId.orEmpty()
                        )
                    )
                }
                .flowOn(Main)
                .launchIn(viewModelScope)
        }
    }

    private fun initNunchuk() = initNunchukUseCase.execute(
        accountId = accountManager.getAccount().email
    ).flowOn(IO).onException { event(SignInErrorEvent(message = it.message)) }

    fun storeStaySignedIn(staySignedIn: Boolean) {
        this.staySignedIn = staySignedIn
    }

}