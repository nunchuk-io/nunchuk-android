package com.nunchuk.android.transaction.receive.address

import com.nunchuk.android.model.Amount

data class UsedAddressModel(
    val address: String = "",
    val balance: Amount = Amount.ZER0
)