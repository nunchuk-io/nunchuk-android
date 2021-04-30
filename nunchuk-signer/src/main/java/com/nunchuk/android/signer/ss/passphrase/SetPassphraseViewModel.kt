package com.nunchuk.android.signer.ss.passphrase

import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.signer.ss.passphrase.SetPassphraseEvent.*
import javax.inject.Inject

internal class SetPassphraseViewModel @Inject constructor(

) : NunchukViewModel<SetPassphraseState, SetPassphraseEvent>() {

    private lateinit var mnemonic: String

    private lateinit var signerName: String

    override val initialState = SetPassphraseState()

    fun init(mnemonic: String, signerName: String) {
        this.mnemonic = mnemonic
        this.signerName = signerName
    }

    fun updatePassphrase(passphrase: String) {
        updateState { copy(passphrase = passphrase) }
    }

    fun updateConfirmPassphrase(confirmPassphrase: String) {
        updateState { copy(confirmPassphrase = confirmPassphrase) }
    }

    fun skipPassphraseEvent() {
        event(PassphraseCompletedEvent(skip = true))
    }

    fun confirmPassphraseEvent() {
        val state = getState()
        val passphrase = state.passphrase
        val confirmPassphrase = state.confirmPassphrase
        when {
            passphrase.isEmpty() -> event(PassPhraseRequiredEvent)
            confirmPassphrase.isEmpty() -> event(ConfirmPassPhraseRequiredEvent)
            passphrase != confirmPassphrase -> event(ConfirmPassPhraseNotMatchedEvent)
            else -> event(PassphraseCompletedEvent(skip = false)) //TODO
        }
    }

}