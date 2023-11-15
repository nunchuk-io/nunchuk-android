package com.nunchuk.android.usecase.byzantine

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.GroupChat
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreateOrUpdateGroupChatUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<CreateOrUpdateGroupChatUseCase.Param, GroupChat>(ioDispatcher) {

    override suspend fun execute(parameters: Param): GroupChat {
        return repository.createOrUpdateGroupChat(
            roomId = parameters.roomId,
            groupId = parameters.groupId,
            historyPeriodId = parameters.historyPeriodId
        )
    }

    class Param(val groupId: String, val roomId: String, val historyPeriodId: String? = null)
}
