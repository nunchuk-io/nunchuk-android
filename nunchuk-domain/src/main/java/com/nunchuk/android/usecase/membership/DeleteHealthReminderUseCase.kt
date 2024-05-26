package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class DeleteHealthReminderUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<DeleteHealthReminderUseCase.Params, Unit>(ioDispatcher) {

    override suspend fun execute(parameters: Params) {
        repository.deleteHealthReminder(parameters.groupId, parameters.walletId, parameters.xfps)
    }

    class Params(val groupId: String?, val walletId: String, val xfps: List<String>)
}