package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

class GetPendingWalletNotifyCountUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<List<String>, HashMap<String, Int>>(ioDispatcher) {

    override suspend fun execute(parameters: List<String>): HashMap<String, Int> {
        val map = HashMap<String, Int>()
        supervisorScope {
            parameters.forEach { groupId ->
                val result = async { repository.getAlerts(groupId).size }
                kotlin.runCatching {
                    map[groupId] = result.await()
                }
            }
        }
        return map
    }
}