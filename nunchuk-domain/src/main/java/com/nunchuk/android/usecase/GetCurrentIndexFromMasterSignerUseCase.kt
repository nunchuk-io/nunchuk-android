package com.nunchuk.android.usecase

import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType

interface GetCurrentIndexFromMasterSignerUseCase {
    fun execute(
            mastersignerId: String,
            walletType: WalletType,
            addressType: AddressType
    ): Int
}