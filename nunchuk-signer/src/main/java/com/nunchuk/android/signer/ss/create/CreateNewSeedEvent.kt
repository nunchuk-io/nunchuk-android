package com.nunchuk.android.signer.ss.create

sealed class CreateNewSeedEvent {
    data class GenerateMnemonicCodeErrorEvent(val message: String) : CreateNewSeedEvent()
    data class OpenSelectPhraseEvent(val mnemonic: String) : CreateNewSeedEvent()
}

data class CreateNewSeedState(val seeds: List<String> = emptyList())
