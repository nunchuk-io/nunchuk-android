package com.nunchuk.android.main.components.tabs.wallet

import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Wallet
import okhttp3.ResponseBody

internal data class WalletsState(
    val wallets: List<Wallet> = emptyList(),
    val signers: List<SingleSigner> = emptyList(),
    val masterSigners: List<MasterSigner> = emptyList()
)

internal sealed class WalletsEvent {
    data class Loading(val loading: Boolean) : WalletsEvent()
    data class ShowErrorEvent(val message: String) : WalletsEvent()
    object AddWalletEvent : WalletsEvent()
    object ShowSignerIntroEvent : WalletsEvent()
    object WalletEmptySignerEvent : WalletsEvent()
    data class Test(val jsonInfo: String, val responseBody: ResponseBody) : WalletsEvent()
}