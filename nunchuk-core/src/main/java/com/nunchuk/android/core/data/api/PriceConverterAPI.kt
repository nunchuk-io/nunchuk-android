package com.nunchuk.android.core.data.api

import com.nunchuk.android.core.data.model.PriceWrapperResponse
import com.nunchuk.android.core.network.Data
import retrofit2.http.GET

interface PriceConverterAPI {

    @GET("prices")
    suspend fun getPrices(): Data<PriceWrapperResponse>
}