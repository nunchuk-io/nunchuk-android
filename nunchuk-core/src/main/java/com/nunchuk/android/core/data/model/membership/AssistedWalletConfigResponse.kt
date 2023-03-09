package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName

data class AssistedWalletConfigResponse(
    @SerializedName("total_allowed_wallets")
    val totalAllowedWallet : Int = 0,
    @SerializedName("active_wallet_count")
    val activeWalletCount : Int = 0,
    @SerializedName("remaining_wallet_count")
    val remainingWalletCount : Int = 0,
)