package com.nunchuk.android.usecase.byzantine

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class HasGroupWalletsUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<Unit, Boolean>(ioDispatcher) {

    override suspend fun execute(parameters: Unit): Boolean =
         repository.hasGroupWallets()
}