package com.nunchuk.android.core.domain

import com.nunchuk.android.FlowUseCase
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.domain.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAssistedWalletIdFlowUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val ncDataStore: NcDataStore
) : FlowUseCase<Unit, String>(dispatcher) {
    override fun execute(parameters: Unit): Flow<String> = ncDataStore.assistedWalletId
}