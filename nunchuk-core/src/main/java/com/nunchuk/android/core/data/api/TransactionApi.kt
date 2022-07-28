package com.nunchuk.android.core.data.api

import com.nunchuk.android.core.data.model.EstimateFeeResponse
import retrofit2.http.GET

interface TransactionApi {
    @GET("fees/recommended")
    suspend fun getFees(): EstimateFeeResponse
}