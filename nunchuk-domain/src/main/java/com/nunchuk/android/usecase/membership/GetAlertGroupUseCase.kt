package com.nunchuk.android.usecase.membership

import com.nunchuk.android.FlowUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Alert
import com.nunchuk.android.repository.PremiumWalletRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAlertGroupUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : FlowUseCase<GetAlertGroupUseCase.Params, List<Alert>>(ioDispatcher) {

    override fun execute(parameters: Params): Flow<List<Alert>> = repository.getAlerts(groupId = parameters.groupId, walletId = parameters.walletId)

    class Params(val groupId: String? = null, val walletId: String? = null)
}