package com.nunchuk.android.core.domain

import com.nunchuk.android.FlowUseCase
import com.nunchuk.android.core.repository.BtcPriceRepository
import com.nunchuk.android.domain.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLocalBtcPriceFlowUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val repository: BtcPriceRepository
) : FlowUseCase<Unit, Double>(dispatcher) {
    override fun execute(parameters: Unit): Flow<Double> {
        return repository.getLocalPrice()
    }
}