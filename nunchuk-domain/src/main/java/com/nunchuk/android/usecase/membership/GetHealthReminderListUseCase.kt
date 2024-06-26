package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.HealthReminder
import com.nunchuk.android.model.SavedAddress
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetHealthReminderListUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<GetHealthReminderListUseCase.Params, List<HealthReminder>>(ioDispatcher) {

    override suspend fun execute(parameters: Params): List<HealthReminder> =
        repository.getHealthReminders(parameters.groupId, parameters.walletId)

    class Params(val groupId: String?, val walletId: String)
}