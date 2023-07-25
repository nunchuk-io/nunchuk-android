package com.nunchuk.android.usecase.byzantine

import com.nunchuk.android.FlowUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.ByzantineGroupBrief
import com.nunchuk.android.repository.PremiumWalletRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGroupBriefByIdFlowUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : FlowUseCase<String, ByzantineGroupBrief>(ioDispatcher) {

    override fun execute(parameters: String): Flow<ByzantineGroupBrief> {
       return repository.getGroupBriefById(parameters)
    }
}