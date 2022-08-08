package com.nunchuk.android.wallet.shared.components.config

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.RoomWallet

data class SharedWalletConfigState(
    val signerModels: List<SignerModel> = emptyList(),
    val roomWallet: RoomWallet? = null,
    val isSender: Boolean = false
)

sealed class SharedWalletConfigEvent {
    object CreateSharedWalletSuccess : SharedWalletConfigEvent()
}