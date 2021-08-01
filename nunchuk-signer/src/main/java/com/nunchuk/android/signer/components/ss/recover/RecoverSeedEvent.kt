package com.nunchuk.android.signer.components.ss.recover

sealed class RecoverSeedEvent {
    object MnemonicRequiredEvent : RecoverSeedEvent()
    object InvalidMnemonicEvent : RecoverSeedEvent()
    data class UpdateMnemonicEvent(val mnemonic: String) : RecoverSeedEvent()
    data class ValidMnemonicEvent(val mnemonic: String) : RecoverSeedEvent()
    data class CanGoNextStepEvent(val canGoNext: Boolean) : RecoverSeedEvent()
}

data class RecoverSeedState(
    val mnemonic: String = "",
    val suggestions: List<String> = emptyList()
)
