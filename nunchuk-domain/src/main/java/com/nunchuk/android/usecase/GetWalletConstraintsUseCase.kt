package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.WalletConstraints
import com.nunchuk.android.model.byzantine.AssistedMember
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetWalletConstraintsUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<Unit, WalletConstraints>(ioDispatcher) {

    override suspend fun execute(parameters: Unit): WalletConstraints =
         repository.getWalletConstraints()
}