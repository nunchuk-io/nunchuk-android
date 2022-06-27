package com.nunchuk.android.core.domain

import com.nunchuk.android.core.data.model.PriceBTCResponse
import com.nunchuk.android.core.repository.PriceConverterAPIRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface GetPriceConvertBTCUseCase {
    fun execute(): Flow<PriceBTCResponse?>
}

internal class GetPriceConvertBTCUseCaseImpl @Inject constructor(
    private val priceConverterAPIRepository: PriceConverterAPIRepository
) : GetPriceConvertBTCUseCase {

    override fun execute() = priceConverterAPIRepository.execute().map { it.btc }.flowOn(Dispatchers.IO)
}
