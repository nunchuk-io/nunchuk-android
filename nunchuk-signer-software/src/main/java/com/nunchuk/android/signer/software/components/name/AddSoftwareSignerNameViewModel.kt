package com.nunchuk.android.signer.software.components.name

import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.signer.software.components.name.AddSoftwareSignerNameEvent.SignerNameInputCompletedEvent
import com.nunchuk.android.signer.software.components.name.AddSoftwareSignerNameEvent.SignerNameRequiredEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
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