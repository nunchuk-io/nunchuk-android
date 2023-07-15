package com.nunchuk.android.usecase

import com.nunchuk.android.FlowUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.ByzantineGroupBrief
import com.nunchuk.android.repository.PremiumWalletRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGroupBriefsFlowUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : FlowUseCase<Unit, List<ByzantineGroupBrief>>(ioDispatcher) {

    override fun execute(parameters: Unit): Flow<List<ByzantineGroupBrief>> =
         repository.getGroupBriefs()
}