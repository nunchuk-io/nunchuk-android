package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName

internal class GetWalletResponse(
    @SerializedName("is_any_wallet_created")
    val hasWalletCreated: Boolean = false,
    @SerializedName("wallets")
    val wallets: List<WalletDto> = emptyList()
)