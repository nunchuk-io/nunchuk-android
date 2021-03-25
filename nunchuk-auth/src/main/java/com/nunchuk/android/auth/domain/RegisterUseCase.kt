package com.nunchuk.android.auth.domain

import com.nunchuk.android.auth.api.UserTokenResponse
import com.nunchuk.android.auth.data.AuthRepository
import com.nunchuk.android.core.account.AccountInfo
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.model.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface RegisterUseCase {
    suspend fun execute(name: String, email: String): Result<UserTokenResponse>
}

internal class RegisterUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val accountManager: AccountManager
) : RegisterUseCase {

    override suspend fun execute(name: String, email: String) = withContext(Dispatchers.IO) {
        try {
            val result = authRepository.register(name = name, email = email)
            accountManager.storeAccount(AccountInfo(email = email, token = result.token.value))
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
