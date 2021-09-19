package com.nunchuk.android.auth.domain

import com.nunchuk.android.auth.data.AuthRepository
import com.nunchuk.android.core.account.AccountManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface GetCurrentUserUseCase {
    fun execute(): Flow<String>
}

internal class GetCurrentUserUseCaseImpl @Inject constructor(
    private val accountManager: AccountManager,
    private val authRepository: AuthRepository,
) : GetCurrentUserUseCase {

    override fun execute() = authRepository.me()
        .map {
            it.chatId.apply {
                accountManager.storeAccount(accountManager.getAccount().copy(chatId = this, name = it.name, avatarUrl = it.avatar))
            }
        }
}
