package com.nunchuk.android.auth.domain

import com.nunchuk.android.auth.api.UserTokenResponse
import com.nunchuk.android.auth.data.AuthRepository
import com.nunchuk.android.core.account.AccountInfo
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.BaseUseCase
import javax.inject.Inject

interface RegisterUseCase {
    suspend fun execute(name: String, email: String): Result<UserTokenResponse>
}

internal class RegisterUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val accountManager: AccountManager
) : BaseUseCase(), RegisterUseCase {

    override suspend fun execute(name: String, email: String) = exe {
        val userTokenResponse = authRepository.register(name = name, email = email)
        userTokenResponse.also {
            accountManager.storeAccount(AccountInfo(email = email, token = it.tokenId))
        }
    }
}
