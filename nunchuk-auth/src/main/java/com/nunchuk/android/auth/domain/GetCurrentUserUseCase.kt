package com.nunchuk.android.auth.domain

import com.nunchuk.android.auth.data.AuthRepository
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.BaseUseCase
import javax.inject.Inject

interface GetCurrentUserUseCase {
    suspend fun execute(): Result<String>
}

internal class GetCurrentUserUseCaseImpl @Inject constructor(
    private val accountManager: AccountManager,
    private val authRepository: AuthRepository,
) : BaseUseCase(), GetCurrentUserUseCase {

    override suspend fun execute() = exe {
        authRepository.me().chatId.apply {
            accountManager.storeAccount(accountManager.getAccount().copy(chatId = this))
        }
    }

}