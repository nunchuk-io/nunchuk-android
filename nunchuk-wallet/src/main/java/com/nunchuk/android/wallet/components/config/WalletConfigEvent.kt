package com.nunchuk.android.wallet.components.config

sealed class WalletConfigEvent {

    object UpdateNameSuccessEvent : WalletConfigEvent()

    data class UpdateNameErrorEvent(val message: String) : WalletConfigEvent()

    data class OpenDynamicQRScreen(val descriptors: List<String> = emptyList()) : WalletConfigEvent()

    data class WalletDetailsError(val message: String) : WalletConfigEvent()

    object DeleteWalletSuccess : WalletConfigEvent()

    data class UploadWalletConfigEvent(val filePath: String) : WalletConfigEvent()
}