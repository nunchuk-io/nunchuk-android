package com.nunchuk.android.usecase.membership

import com.nunchuk.android.FlowUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.PremiumWalletRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAssistedKeysUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val repository: PremiumWalletRepository,
) : FlowUseCase<Unit, Set<String>>(ioDispatcher) {
    override fun execute(parameters: Unit): Flow<Set<String>> = repository.assistedKeys()
}