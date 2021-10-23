package com.nunchuk.android.auth.domain

import com.nunchuk.android.auth.api.UserTokenResponse
import com.nunchuk.android.auth.data.AuthRepository
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.usecase.BaseUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface VerifyNewDeviceUseCase {
    fun execute(
        email: String,
        loginHalfToken: String,
        pin: String,
        deviceId: String,
        staySignedIn: Boolean
    ): Flow<Pair<String, String>>
}

internal class VerifyNewDeviceUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val accountManager: AccountManager
) : BaseUseCase(), VerifyNewDeviceUseCase {

    override fun execute(
        email: String,
        loginHalfToken: String,
        pin: String,
        deviceId: String,
        staySignedIn: Boolean
    ) = authRepository.verify(
        email = email, loginHalfToken = loginHalfToken, pin = pin, deviceId = deviceId
    ).map {
        storeAccount(email, it, staySignedIn)
    }

    private fun storeAccount(email: String, response: UserTokenResponse, staySignedIn: Boolean): Pair<String, String> {
        val account = accountManager.getAccount()
        accountManager.storeAccount(
            account.copy(
                email = email,
                token = response.tokenId,
                activated = true,
                staySignedIn = staySignedIn
            )
        )
        return response.tokenId to response.deviceId
    }

}