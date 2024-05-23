package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class DeleteSavedAddressUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<DeleteSavedAddressUseCase.Params, Unit>(ioDispatcher) {

    override suspend fun execute(parameters: Params) {
        repository.deleteSavedAddress(address = parameters.address)
    }

    class Params(val address: String)
}