package com.nunchuk.android.usecase

import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType

interface GetUnusedSignerFromMasterSignerUseCase {
    fun execute(
            mastersignerId: String,
            walletType: WalletType,
            addressType: AddressType
    ): SingleSigner
}