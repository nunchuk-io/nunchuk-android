package com.nunchuk.android.usecase

import com.nunchuk.android.FlowUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.util.LoadingOptions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGroupsFlowUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : FlowUseCase<LoadingOptions, List<ByzantineGroup>>(ioDispatcher) {

    override fun execute(parameters: LoadingOptions): Flow<List<ByzantineGroup>> =
        repository.getGroups(parameters)
}