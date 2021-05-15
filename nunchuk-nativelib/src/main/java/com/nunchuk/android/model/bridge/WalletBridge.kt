package com.nunchuk.android.model.bridge

import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Wallet

data class WalletBridge(
    var id: String = "",
    var name: String = "",
    var totalRequireSigns: Int = 0,
    var signers: List<SingleSigner> = emptyList(),
    var addressType: Int,
    var escrow: Boolean = false,
    var balance: Amount = Amount.ZER0,
    var createDate: Long = 0L,
    var description: String = ""
)

internal fun Wallet.toBridge() = WalletBridge(
    id = id,
    name = name,
    totalRequireSigns = totalRequireSigns,
    signers = signers,
    addressType = addressType.ordinal,
    escrow = escrow,
    balance = balance,
    createDate = createDate,
    description = description
)