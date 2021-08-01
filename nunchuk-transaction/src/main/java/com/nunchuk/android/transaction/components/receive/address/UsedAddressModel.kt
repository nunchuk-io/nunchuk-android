package com.nunchuk.android.transaction.components.receive.address

import com.nunchuk.android.model.Amount

data class UsedAddressModel(
    val address: String = "",
    val balance: Amount = Amount.ZER0
)