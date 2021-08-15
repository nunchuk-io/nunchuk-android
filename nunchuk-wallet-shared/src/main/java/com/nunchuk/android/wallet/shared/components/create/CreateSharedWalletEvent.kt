package com.nunchuk.android.wallet.shared.components.create

import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.AddressType.NATIVE_SEGWIT
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.type.WalletType.MULTI_SIG

sealed class AddSharedWalletEvent {
    object WalletNameRequiredEvent : AddSharedWalletEvent()
    data class WalletSetupDoneEvent(
        val walletName: String,
        val walletType: WalletType,
        val addressType: AddressType
    ) : AddSharedWalletEvent()
}

data class AddSharedWalletState(
    val walletName: String = "",
    val walletType: WalletType = MULTI_SIG,
    val addressType: AddressType = NATIVE_SEGWIT
)