package com.nunchuk.android.core.repository

import com.nunchuk.android.core.data.api.PriceConverterAPI
import com.nunchuk.android.core.data.model.PriceResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface PriceConverterAPIRepository {
    fun execute(
    ): Flow<PriceResponse>
}

internal class PriceConverterAPIRepositoryImpl @Inject constructor(
    private val priceConverterAPI: PriceConverterAPI
) : PriceConverterAPIRepository {

    override fun execute(
    ) = flow {
        emit(
            priceConverterAPI.getPrices()
        )
    }.map {
        it.data.prices
    }

}