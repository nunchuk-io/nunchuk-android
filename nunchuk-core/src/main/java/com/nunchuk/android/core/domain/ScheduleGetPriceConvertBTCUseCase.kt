package com.nunchuk.android.core.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface ScheduleGetPriceConvertBTCUseCase {
    fun execute(): Flow<Unit>
}

internal class ScheduleGetPriceConvertBTCUseCaseImpl @Inject constructor(
) : ScheduleGetPriceConvertBTCUseCase {

    override fun execute(
    ) = flow {
        delay(0)
        while (true) {
            emit(Unit)
            delay(INTERVAL_TIME_GET_BTC_PRICE)
        }

    }

    companion object {
        private const val INTERVAL_TIME_GET_BTC_PRICE = 300000L
    }
}