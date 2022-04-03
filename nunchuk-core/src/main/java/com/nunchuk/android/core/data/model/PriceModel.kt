package com.nunchuk.android.core.data.model

import com.google.gson.annotations.SerializedName

data class PriceWrapperResponse(
    @SerializedName("prices")
    val prices: PriceResponse
)

data class PriceResponse(
    @SerializedName("BTC")
    val btc: PriceBTCResponse? = null
)

data class PriceBTCResponse(
    @SerializedName("USD")
    val usd: Double? = null
)
