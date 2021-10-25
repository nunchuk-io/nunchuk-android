package com.nunchuk.android.core.profile

import com.nunchuk.android.core.account.AccountManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface UpdateUseProfileUseCase {
    fun execute(name: String?, avatarUrl: String?): Flow<String>
}

internal class UpdateUseProfileUseCaseImpl @Inject constructor(
    private val accountManager: AccountManager,
    private val userProfileRepository: UserProfileRepository
) : UpdateUseProfileUseCase {

    override fun execute(name: String?, avatarUrl: String?) = userProfileRepository.updateUserProfile(name, avatarUrl)
        .map {
            accountManager.storeAccount(accountManager.getAccount().copy(name = it.name.orEmpty(), avatarUrl = it.avatar.orEmpty()))
            it.chatId.orEmpty()
        }
}
