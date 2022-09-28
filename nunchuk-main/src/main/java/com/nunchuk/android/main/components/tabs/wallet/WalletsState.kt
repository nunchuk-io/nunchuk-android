package com.nunchuk.android.main.components.tabs.wallet

import com.nunchuk.android.model.*
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.ConnectionStatus

internal data class WalletsState(
    val wallets: List<WalletExtended> = emptyList(),
    val signers: List<SingleSigner> = emptyList(),
    val masterSigners: List<MasterSigner> = emptyList(),
    val connectionStatus: ConnectionStatus? = null,
    val chain: Chain = Chain.MAIN
)

internal sealed class WalletsEvent {
    data class Loading(val loading: Boolean) : WalletsEvent()
    data class ShowErrorEvent(val e: Throwable?) : WalletsEvent()
    object AddWalletEvent : WalletsEvent()
    object ShowSignerIntroEvent : WalletsEvent()
    object WalletEmptySignerEvent : WalletsEvent()
    class NeedSetupSatsCard(val status: SatsCardStatus) : WalletsEvent()
    class NfcLoading(val loading: Boolean) : WalletsEvent()
    class GoToSatsCardScreen(val status: SatsCardStatus) : WalletsEvent()
    class GetTapSignerStatusSuccess(val status: TapSignerStatus) : WalletsEvent()
    class SatsCardUsedUp(val numberOfSlot: Int) : WalletsEvent()
}