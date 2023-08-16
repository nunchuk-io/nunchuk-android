package com.nunchuk.android.usecase.byzantine

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.byzantine.KeyHealthStatus
import com.nunchuk.android.repository.GroupWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetGroupWalletKeyHealthStatusUseCase @Inject constructor(
    private val repository: GroupWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<GetGroupWalletKeyHealthStatusUseCase.Params, List<KeyHealthStatus>>(ioDispatcher) {

    override suspend fun execute(parameters: Params): List<KeyHealthStatus> {
        return repository.getWalletHealthStatus(
            groupId = parameters.groupId,
            walletId = parameters.walletId
        )
    }

    data class Params(
        val groupId: String,
        val walletId: String
    )
}