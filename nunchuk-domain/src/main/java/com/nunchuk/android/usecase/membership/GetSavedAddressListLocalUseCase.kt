package com.nunchuk.android.usecase.membership

import com.nunchuk.android.FlowUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SavedAddress
import com.nunchuk.android.repository.PremiumWalletRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSavedAddressListLocalUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : FlowUseCase<Unit, List<SavedAddress>>(ioDispatcher) {
    override fun execute(parameters: Unit): Flow<List<SavedAddress>> {
        return repository.getSavedAddressesLocal()
    }
}