package com.nunchuk.android.auth.domain

import com.nunchuk.android.auth.data.AuthRepository
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface RecoverPasswordUseCase {
    suspend fun execute(
        emailAddress: String?,
        oldPassword: String,
        newPassword: String,
        confirmPassword: String
    ): Result<Unit>
}

internal class RecoverPasswordUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val accountManager: AccountManager
) : RecoverPasswordUseCase {

    override suspend fun execute(
        emailAddress: String?,
        oldPassword: String,
        newPassword: String,
        confirmPassword: String
    ) = withContext(Dispatchers.IO) {
        try {
            val account = accountManager.getAccount()
            authRepository.recoverPassword(
                emailAddress = emailAddress ?: account.email,
                oldPassword = oldPassword,
                newPassword = newPassword,
                confirmPassword = confirmPassword
            )
            accountManager.storeAccount(account.copy(activated = true))
            Success(Unit)
        } catch (e: Exception) {
            Error(e)
        }
    }
}