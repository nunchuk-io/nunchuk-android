package com.nunchuk.android.model

import com.nunchuk.android.type.AddressType

data class Wallet(
        val id: String,
        val name: String,
        val m: Int,
        val n: Int,
        val signer: List<SingleSigner>,
        val addressType: AddressType,
        val escrow: Boolean,
        val balance: Double,
        val createDate: Long,
        val description: String

)