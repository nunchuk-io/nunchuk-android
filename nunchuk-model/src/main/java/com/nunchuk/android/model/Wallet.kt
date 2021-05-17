package com.nunchuk.android.model

import com.nunchuk.android.type.AddressType

data class Wallet(
    var id: String = "",
    var name: String = "",
    var totalRequireSigns: Int = 0,
    var signers: List<SingleSigner> = emptyList(),
    var addressType: AddressType = AddressType.NESTED_SEGWIT,
    var escrow: Boolean = false,
    var balance: Amount = Amount.ZER0,
    var createDate: Long = 0L,
    var description: String = ""
) {
    companion object {
        val EMPTY = Wallet()
    }
}