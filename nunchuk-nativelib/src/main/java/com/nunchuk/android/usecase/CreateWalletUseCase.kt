package com.nunchuk.android.usecase

import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.type.AddressType

interface CreateWalletUseCase {
    fun execute(
            name: String,
            totalRequireSigns: Int,
            totalSigners: Int,
            signers: List<SingleSigner>,
            addressType: AddressType,
            isEscrow: Boolean,
            description: String = ""
    ): Wallet
}