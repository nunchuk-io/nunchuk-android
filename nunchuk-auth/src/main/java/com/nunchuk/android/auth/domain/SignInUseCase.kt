package com.nunchuk.android.auth.domain

import com.nunchuk.android.auth.api.UserTokenResponse
import com.nunchuk.android.auth.data.AuthRepository
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.BaseUseCase
import javax.inject.Inject

interface SignInUseCase {
    suspend fun execute(
        email: String,
        password: String,
        staySignedIn: Boolean = true
    ): Result<String>
}

internal class SignInUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val accountManager: AccountManager
) : BaseUseCase(), SignInUseCase {

    override suspend fun execute(email: String, password: String, staySignedIn: Boolean) = exe {
        val tokenResponse = authRepository.login(
            email = email,
            password = password
        ).apply { storeAccount(email, this, staySignedIn) }
        tokenResponse.tokenId
    }

    private fun storeAccount(email: String, response: UserTokenResponse, staySignedIn: Boolean) {
        val account = accountManager.getAccount()
        accountManager.storeAccount(
            account.copy(
                email = email,
                token = response.tokenId,
                activated = true,
                staySignedIn = staySignedIn
            )
        )
    }

}