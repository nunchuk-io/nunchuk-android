package com.nunchuk.android.auth.components.verify

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.auth.components.verify.VerifyNewDeviceEvent.*
import com.nunchuk.android.auth.domain.VerifyNewDeviceUseCase
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.share.InitNunchukUseCase
import com.nunchuk.android.utils.onException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject

internal class VerifyNewDeviceViewModel @Inject constructor(
    private val verifyNewDeviceUseCase: VerifyNewDeviceUseCase,
    private val initNunchukUseCase: InitNunchukUseCase,
    private val accountManager: AccountManager
) : NunchukViewModel<Unit, VerifyNewDeviceEvent>() {

    override val initialState = Unit

    private var token: String? = null
    private var encryptedDeviceId: String? = null

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
            .flatMapConcat {
                token = it.first
                encryptedDeviceId = it.second
                initNunchuk()
            }
            .onEach {
                event(SignInSuccessEvent(token = token.orEmpty(), encryptedDeviceId = encryptedDeviceId.orEmpty()))
            }
            .flowOn(Dispatchers.Main)
            .launchIn(viewModelScope)
    }

    private fun initNunchuk(): Flow<Unit> {
        val account = accountManager.getAccount()
        // TODO: use a real passphrase; make sure to use the same passphrase on ALL InitNunchukUseCase instances
        // or the user will lose access to their keys/wallets
        return initNunchukUseCase.execute(accountId = account.email)
            .flowOn(Dispatchers.IO)
            .onException { event(SignInErrorEvent(message = it.message.orUnknownError())) }
    }

}