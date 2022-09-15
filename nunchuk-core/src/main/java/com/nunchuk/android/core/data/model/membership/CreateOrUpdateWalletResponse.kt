package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName

internal class CreateOrUpdateWalletResponse(
    @SerializedName("wallet")
    val wallet: WalletDto
)