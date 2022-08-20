package com.nunchuk.android.core.domain

import com.nunchuk.android.core.data.model.PriceBTCResponse
import com.nunchuk.android.core.repository.BtcPriceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface GetRemotePriceConvertBTCUseCase {
    fun execute(): Flow<PriceBTCResponse?>
}

internal class GetRemotePriceConvertBTCUseCaseImpl @Inject constructor(
    private val btcPriceRepository: BtcPriceRepository,
) : GetRemotePriceConvertBTCUseCase {

    override fun execute() = btcPriceRepository.getRemotePrice().map {
        it.btc
    }.flowOn(Dispatchers.IO)
}
