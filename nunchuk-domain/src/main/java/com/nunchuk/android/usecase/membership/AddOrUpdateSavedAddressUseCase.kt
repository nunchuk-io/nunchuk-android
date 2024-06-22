package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class AddOrUpdateSavedAddressUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<AddOrUpdateSavedAddressUseCase.Params, Unit>(ioDispatcher) {

    override suspend fun execute(parameters: Params) {
        repository.addOrUpdateSavedAddress(address = parameters.address, label = parameters.label, isPremiumUser = parameters.isPremiumUser)
    }

    class Params(val address: String, val label: String, val isPremiumUser: Boolean)
}