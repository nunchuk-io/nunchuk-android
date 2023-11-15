package com.nunchuk.android.usecase.byzantine

import com.nunchuk.android.FlowUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.repository.PremiumWalletRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetGroupUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : FlowUseCase<GetGroupUseCase.Params, ByzantineGroup>(ioDispatcher) {

    override fun execute(parameters: Params) =
        repository.getGroup(parameters.groupId)

    class Params(val groupId: String)
}