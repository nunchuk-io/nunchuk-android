package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

class GetPendingWalletNotifyCountUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<List<String>, Map<String, Int>>(ioDispatcher) {

    override suspend fun execute(parameters: List<String>): Map<String, Int> {
        return supervisorScope {
            parameters.map { groupId ->
                async { groupId to repository.getAlertTotal(groupId) }
            }.awaitAll().toMap()
        }
    }
}