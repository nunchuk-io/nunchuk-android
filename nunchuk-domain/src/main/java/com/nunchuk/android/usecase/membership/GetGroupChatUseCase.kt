package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Alert
import com.nunchuk.android.model.GroupChat
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetGroupChatUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<String, GroupChat>(ioDispatcher) {

    override suspend fun execute(parameters: String): GroupChat {
        return repository.getGroupChat(parameters)
    }
}