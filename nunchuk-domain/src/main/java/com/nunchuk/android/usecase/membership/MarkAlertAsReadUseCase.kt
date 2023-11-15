package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Alert
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class MarkAlertAsReadUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<MarkAlertAsReadUseCase.Param, Unit>(ioDispatcher) {

    override suspend fun execute(parameters: Param) {
        return repository.markAlertAsRead(alertId = parameters.alertId, groupId = parameters.groupId)
    }

    class Param(val alertId: String, val groupId: String)
}