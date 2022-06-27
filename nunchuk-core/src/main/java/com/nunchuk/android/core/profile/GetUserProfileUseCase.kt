package com.nunchuk.android.core.profile

import com.nunchuk.android.core.account.AccountManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface GetUserProfileUseCase {
    fun execute(): Flow<String>
}

internal class GetUserProfileUseCaseImpl @Inject constructor(
    private val accountManager: AccountManager,
    private val userProfileRepository: UserProfileRepository
) : GetUserProfileUseCase {

    override fun execute() = userProfileRepository.getUserProfile().map {
        it.chatId.orEmpty().apply {
            accountManager.storeAccount(
                accountManager.getAccount()
                    .copy(chatId = this, name = it.name.orEmpty(), avatarUrl = it.avatar.orEmpty())
            )
        }
    }.flowOn(Dispatchers.IO)
}
