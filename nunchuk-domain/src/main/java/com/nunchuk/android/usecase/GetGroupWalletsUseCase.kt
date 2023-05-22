package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.repository.PremiumWalletRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetGroupWalletsUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<Unit, List<ByzantineGroup>>(ioDispatcher) {

    override suspend fun execute(parameters: Unit): List<ByzantineGroup> =
         repository.getGroupWallets()
}