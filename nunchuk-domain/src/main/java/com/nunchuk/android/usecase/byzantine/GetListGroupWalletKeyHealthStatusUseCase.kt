package com.nunchuk.android.usecase.byzantine

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.byzantine.KeyHealthStatus
import com.nunchuk.android.repository.GroupWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

class GetListGroupWalletKeyHealthStatusUseCase @Inject constructor(
    private val repository: GroupWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<GetListGroupWalletKeyHealthStatusUseCase.Params, Map<String, List<KeyHealthStatus>>>(
    ioDispatcher
) {

    override suspend fun execute(parameters: Params): Map<String, List<KeyHealthStatus>> {
        return supervisorScope {
            parameters.pairGroupWallets.map { (groupId, walletId) ->
                async {
                    walletId to repository.getWalletHealthStatusRemote(
                        groupId = groupId,
                        walletId = walletId,
                    )
                }
            }.awaitAll().toMap()
        }
    }

    data class Params(
        val pairGroupWallets: List<Pair<String, String>>
    )
}