package com.nunchuk.android.usecase

import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.type.AddressType

interface CreateWalletUseCase {
    fun execute(
            name: String,
            m: Int,
            n: Int,
            signers: List<SingleSigner>,
            address_type: AddressType,
            is_escrow: Boolean,
            description: String = ""
    ): Wallet
}