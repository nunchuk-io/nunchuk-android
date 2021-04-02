package com.nunchuk.android.auth.domain

import com.nunchuk.android.auth.api.UserTokenResponse
import com.nunchuk.android.auth.data.AuthRepository
import com.nunchuk.android.core.account.AccountManagerImpl
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.BaseUseCase
import javax.inject.Inject

interface SignInUseCase {
    suspend fun execute(
        email: String,
        password: String,
        staySignedIn: Boolean = true
    ): Result<UserTokenResponse>
}

internal class SignInUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val accountManager: AccountManagerImpl
) : BaseUseCase(), SignInUseCase {

    override suspend fun execute(
        email: String,
        password: String,
        staySignedIn: Boolean
    ) = exe {
        authRepository.login(email = email, password = password).apply {
            accountManager.storeAccount(
                accountManager.getAccount().copy(email = email, token = tokenId, activated = true, staySignedIn = staySignedIn)
            )
        }
    }

}