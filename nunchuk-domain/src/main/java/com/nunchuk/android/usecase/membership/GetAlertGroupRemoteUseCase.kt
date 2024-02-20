package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Alert
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetAlertGroupRemoteUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<GetAlertGroupRemoteUseCase.Params, List<Alert>>(ioDispatcher) {

    override suspend fun execute(parameters: Params): List<Alert> =
        repository.getAlertsRemote(groupId = parameters.groupId, walletId = parameters.walletId)

    class Params(val groupId: String?, val walletId: String?)
}