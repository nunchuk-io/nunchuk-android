package com.nunchuk.android.wallet.shared.components.recover

import com.nunchuk.android.model.Wallet
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.AddressType.NATIVE_SEGWIT
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.type.WalletType.MULTI_SIG

sealed class RecoverSharedWalletEvent {
    data class RecoverSharedWalletSuccess(
        val wallet: Wallet = Wallet(),
    ) : RecoverSharedWalletEvent()

    object WalletNameRequiredEvent : RecoverSharedWalletEvent()
    data class WalletSetupDoneEvent(
        val walletName: String
    ) : RecoverSharedWalletEvent()
}

data class RecoverSharedWalletState(
    val walletName: String = "",
)