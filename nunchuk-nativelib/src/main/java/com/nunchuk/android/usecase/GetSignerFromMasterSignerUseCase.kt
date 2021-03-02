package com.nunchuk.android.usecase

import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType

interface GetSignerFromMasterSignerUseCase {
    fun execute(
            mastersignerId: String,
            walletType: WalletType,
            addressType: AddressType,
            index: Int = 0
    ): SingleSigner
}