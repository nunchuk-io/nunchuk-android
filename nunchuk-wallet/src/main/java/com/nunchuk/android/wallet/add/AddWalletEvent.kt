package com.nunchuk.android.wallet.add

import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.AddressType.*
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.type.WalletType.*

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