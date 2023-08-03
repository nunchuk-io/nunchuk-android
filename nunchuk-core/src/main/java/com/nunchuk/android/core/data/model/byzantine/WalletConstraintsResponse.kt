package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.annotations.SerializedName

internal class WalletConstraintsDataResponse(
    @SerializedName("constraints")
    val constraints: List<WalletConstraintsResponse>? = null
)

internal class WalletConstraintsResponse(
    @SerializedName("maximum_keyholder")
    val maximumKeyholder: Int? = null,
    @SerializedName("wallet_config")
    val walletConfig: WalletConfigDto? = null
)