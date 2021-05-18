package com.nunchuk.android.transaction.receive.address.used

import com.nunchuk.android.model.Amount

data class UsedAddressModel(
    val address: String = "",
    val balance: Amount = Amount.ZER0
)