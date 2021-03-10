package com.nunchuk.android.model

import com.nunchuk.android.type.AddressType

data class Wallet(
        val id: String = "",
        val name: String = "",
        val m: Int = 0,
        val n: Int = 0,
        val signer: List<SingleSigner> = emptyList(),
        val addressType: AddressType = AddressType.ANY,
        val escrow: Boolean = false,
        val balance: Double = 0.0,
        val createDate: Long = 0L,
        val description: String = ""

)