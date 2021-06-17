package com.nunchuk.android.auth.domain

import com.nunchuk.android.auth.data.AuthRepository
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.BaseUseCase
import javax.inject.Inject

interface RecoverPasswordUseCase {
    suspend fun execute(
        emailAddress: String?,
        oldPassword: String,
        newPassword: String
    ): Result<Unit>
}

internal class RecoverPasswordUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val accountManager: AccountManager
) : BaseUseCase(), RecoverPasswordUseCase {

    override suspend fun execute(
        emailAddress: String?,
        oldPassword: String,
        newPassword: String
    ) = exe {
        val account = accountManager.getAccount()
        authRepository.recoverPassword(
            email = emailAddress ?: account.email,
            oldPassword = oldPassword,
            newPassword = newPassword
        )
        accountManager.storeAccount(account.copy(activated = true))
    }
}