package com.nunchuk.android.usecase.byzantine

import com.nunchuk.android.FlowUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.byzantine.KeyHealthStatus
import com.nunchuk.android.repository.GroupWalletRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGroupWalletKeyHealthStatusUseCase @Inject constructor(
    private val repository: GroupWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : FlowUseCase<GetGroupWalletKeyHealthStatusUseCase.Params, List<KeyHealthStatus>>(ioDispatcher) {

    override fun execute(parameters: Params): Flow<List<KeyHealthStatus>> {
        return repository.getWalletHealthStatus(
            groupId = parameters.groupId,
            walletId = parameters.walletId,
        )
    }

    data class Params(
        val groupId: String,
        val walletId: String,
    )
}