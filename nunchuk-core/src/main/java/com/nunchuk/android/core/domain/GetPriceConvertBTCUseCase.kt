package com.nunchuk.android.core.domain

import com.nunchuk.android.core.api.PriceBTCResponse
import com.nunchuk.android.core.data.PriceConverterAPIRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface GetPriceConvertBTCUseCase {
    fun execute(): Flow<PriceBTCResponse?>
}

internal class GetPriceConvertBTCUseCaseImpl @Inject constructor(
    private val priceConverterAPIRepository: PriceConverterAPIRepository
) : GetPriceConvertBTCUseCase {

    override fun execute(
    ) = priceConverterAPIRepository.execute().map {
        it.btc
    }
}
