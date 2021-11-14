package com.nunchuk.android.auth.components.verify

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.auth.components.verify.VerifyNewDeviceEvent.*
import com.nunchuk.android.auth.domain.GetCurrentUserUseCase
import com.nunchuk.android.auth.domain.LoginWithMatrixUseCase
import com.nunchuk.android.auth.domain.VerifyNewDeviceUseCase
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.share.InitNunchukUseCase
import com.nunchuk.android.utils.onException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

internal class VerifyNewDeviceViewModel @Inject constructor(
    private val verifyNewDeviceUseCase: VerifyNewDeviceUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val loginWithMatrixUseCase: LoginWithMatrixUseCase,
    private val initNunchukUseCase: InitNunchukUseCase,
    private val accountManager: AccountManager
) : NunchukViewModel<Unit, VerifyNewDeviceEvent>() {

    override val initialState = Unit

    fun handleVerifyNewDevice(
        email: String,
        loginHalfToken: String,
        pin: String,
        deviceId: String,
        staySignedIn: Boolean
    ) {
        verifyNewDeviceUseCase.execute(
            email = email,
            loginHalfToken = loginHalfToken,
            pin = pin,
            deviceId = deviceId,
            staySignedIn = staySignedIn
        ).flowOn(Dispatchers.IO)
            .onStart { event(ProcessingEvent) }
            .onException { event(SignInErrorEvent(message = it.message.orUnknownError())) }
            .flatMapConcat { getCurrentUser(token = it.first, encryptedDeviceId = it.second) }
            .onEach { event(SignInSuccessEvent) }
            .flowOn(Dispatchers.Main)
            .launchIn(viewModelScope)
    }

    private fun getCurrentUser(token: String, encryptedDeviceId: String): Flow<Unit> {
        return getCurrentUserUseCase.execute()
            .flatMapConcat { loginWithMatrix(userName = it, password = token, encryptedDeviceId = encryptedDeviceId) }
            .flatMapConcat { initNunchuk() }
    }

    private fun initNunchuk(): Flow<Unit> {
        val account = accountManager.getAccount()
        // TODO: use a real passphrase; make sure to use the same passphrase on ALL InitNunchukUseCase instances
        // or the user will lose access to their keys/wallets
        return initNunchukUseCase.execute("", account.email)
            .onException { event(SignInErrorEvent(message = it.message.orUnknownError())) }
    }

    private fun loginWithMatrix(userName: String, password: String, encryptedDeviceId: String): Flow<Session> {
        return loginWithMatrixUseCase.execute(userName = userName, password = password, encryptedDeviceId = encryptedDeviceId)
            .onException { event(SignInErrorEvent(message = it.message.orUnknownError())) }
            .onEach {
                SessionHolder.storeActiveSession(it)
            }
    }
}