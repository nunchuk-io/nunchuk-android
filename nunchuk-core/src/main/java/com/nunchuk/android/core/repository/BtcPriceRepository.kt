package com.nunchuk.android.core.repository

import com.nunchuk.android.core.data.api.PriceConverterAPI
import com.nunchuk.android.core.data.model.PriceResponse
import com.nunchuk.android.core.persistence.NcDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface BtcPriceRepository {
    fun getRemotePrice(): Flow<PriceResponse>
    fun getLocalPrice(): Flow<Double>
}

internal class BtcPriceRepositoryImpl @Inject constructor(
    private val priceConverterAPI: PriceConverterAPI,
    private val ncDataStore: NcDataStore
) : BtcPriceRepository {

    override fun getRemotePrice(
    ) = flow {
        emit(
            priceConverterAPI.getPrices()
        )
    }.map {
        it.data.prices.btc?.usd?.let { priceUsd ->
            ncDataStore.updateBtcPrice(priceUsd)
        }
        it.data.prices
    }

    override fun getLocalPrice(): Flow<Double> = ncDataStore.btcPriceFlow
}