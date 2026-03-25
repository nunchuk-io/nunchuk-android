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
) : UseCase<GetPendingWalletNotifyCountUseCase.Param, Map<String, Int>>(ioDispatcher) {

    override suspend fun execute(parameters: Param): Map<String, Int> {
        if (parameters.walletIds.isEmpty() && parameters.freeGroupWalletIds.isEmpty() && parameters.groupIds.isEmpty()) return emptyMap()
        return supervisorScope {
            val walletAlerts = parameters.walletIds.map { walletId ->
                async {
                    walletId to runCatching { repository.getAlertTotal(walletId = walletId) }.getOrDefault(
                        0
                    )
                }
            }.awaitAll().toMap()
            val groupAlerts = parameters.groupIds.map { groupId ->
                async {
                    groupId to runCatching { repository.getAlertTotal(groupId = groupId) }.getOrDefault(
                        0
                    )
                }
            }.awaitAll().toMap()
            val freeGroupWalletAlerts = parameters.freeGroupWalletIds.map { walletId ->
                async {
                    walletId to runCatching {
                        repository.getAlertTotal(
                            walletId = walletId,
                            isFreeGroupWallet = true
                        )
                    }.getOrDefault(
                        0
                    )
                }
            }.awaitAll().toMap()
            walletAlerts + groupAlerts + freeGroupWalletAlerts
        }
    }

    class Param(
        val groupIds: List<String>,
        val walletIds: List<String>,
        val freeGroupWalletIds: List<String> = emptyList(),
    )
}