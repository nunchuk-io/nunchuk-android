package com.nunchuk.android.auth.domain

import com.nunchuk.android.auth.api.UserTokenResponse
import com.nunchuk.android.auth.data.AuthRepository
import com.nunchuk.android.core.account.AccountInfo
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    private val accountManager: AccountManager
) : SignInUseCase {

    override suspend fun execute(
        email: String,
        password: String,
        staySignedIn: Boolean
    ) = withContext(Dispatchers.IO) {
        try {
            val result = authRepository.login(email = email, password = password)
            val account = accountManager.getAccount()
            accountManager.storeAccount(account.copy(
                email = email,
                token = result.token.value,
                activated = true,
                staySignedIn = staySignedIn
            ))
            Success(result)
        } catch (e: Exception) {
            Error(e)
        }
    }

}