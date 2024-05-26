package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class AddOrUpdateHealthReminderUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<AddOrUpdateHealthReminderUseCase.Params, Unit>(ioDispatcher) {

    override suspend fun execute(parameters: Params) {
        repository.addOrUpdateHealthReminder(
            parameters.groupId,
            parameters.walletId,
            parameters.xfps,
            parameters.frequency,
            parameters.startDateMillis
        )
    }

    class Params(
        val groupId: String?,
        val walletId: String,
        val xfps: List<String>,
        val frequency: String,
        val startDateMillis: Long
    )
}