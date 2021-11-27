package com.nunchuk.android.main.components.tabs.wallet

import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.ConnectionStatus

internal data class WalletsState(
    val wallets: List<Wallet> = emptyList(),
    val signers: List<SingleSigner> = emptyList(),
    val masterSigners: List<MasterSigner> = emptyList(),
    val connectionStatus: ConnectionStatus? = null,
    val chain: Chain = Chain.MAIN
)

internal sealed class WalletsEvent {
    data class GetConnectionStatusSuccessEvent(
        val connectionStatus: ConnectionStatus
    ) : WalletsEvent()
    data class Loading(val loading: Boolean) : WalletsEvent()
    data class ShowErrorEvent(val message: String) : WalletsEvent()
    object AddWalletEvent : WalletsEvent()
    object ShowSignerIntroEvent : WalletsEvent()
    object WalletEmptySignerEvent : WalletsEvent()
}