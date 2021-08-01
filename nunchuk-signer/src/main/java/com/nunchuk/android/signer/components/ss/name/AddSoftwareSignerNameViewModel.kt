package com.nunchuk.android.signer.components.ss.name

import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.signer.components.ss.name.AddSoftwareSignerNameEvent.SignerNameInputCompletedEvent
import com.nunchuk.android.signer.components.ss.name.AddSoftwareSignerNameEvent.SignerNameRequiredEvent
import javax.inject.Inject

internal class AddSoftwareSignerNameViewModel @Inject constructor(
) : NunchukViewModel<AddSoftwareSignerNameState, AddSoftwareSignerNameEvent>() {

    override val initialState = AddSoftwareSignerNameState()

    init {
        updateSignerName("")
    }

    fun updateSignerName(signerName: String) {
        updateState { copy(signerName = signerName) }
    }

    fun handleContinue() {
        val signerName = getState().signerName
        if (signerName.isBlank()) {
            event(SignerNameRequiredEvent)
        } else {
            event(SignerNameInputCompletedEvent(signerName))
        }
    }

}