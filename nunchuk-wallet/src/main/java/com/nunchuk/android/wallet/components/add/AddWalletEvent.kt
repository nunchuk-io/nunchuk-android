package com.nunchuk.android.wallet.components.add

import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.AddressType.NESTED_SEGWIT
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.type.WalletType.MULTI_SIG

sealed class AddWalletEvent {
    object WalletNameRequiredEvent : AddWalletEvent()
    data class WalletSetupDoneEvent(
        val walletName: String,
        val walletType: WalletType,
        val addressType: AddressType
    ) : AddWalletEvent()
}

data class AddWalletState(
    val walletName: String = "",
    val walletType: WalletType = MULTI_SIG,
    val addressType: AddressType = NESTED_SEGWIT
)