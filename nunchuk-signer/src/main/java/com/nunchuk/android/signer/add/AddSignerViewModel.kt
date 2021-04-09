package com.nunchuk.android.signer.add

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Result
import com.nunchuk.android.signer.add.AddSignerEvent.*
import com.nunchuk.android.signer.util.InvalidSignerFormatException
import com.nunchuk.android.signer.util.SignerInput
import com.nunchuk.android.signer.util.toSigner
import com.nunchuk.android.usecase.CreateSignerUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class AddSignerViewModel @Inject constructor(
    private val createSignerUseCase: CreateSignerUseCase
) : NunchukViewModel<Unit, AddSignerEvent>() {

    override val initialState = Unit

    fun handleAddSigner(signerName: String, signerSpec: String) {
        validateInput(signerName, signerSpec) {
            doAfterValidate(signerName, signerSpec, it)
        }
    }

    private fun doAfterValidate(signerName: String, signerSpec: String, signerInput: SignerInput) {
        viewModelScope.launch {
            val result = createSignerUseCase.execute(
                name = signerName,
                xpub = signerInput.xpub,
                derivationPath = signerInput.path,
                masterFingerprint = signerInput.fingerPrint,
                publicKey = ""
            )
            if (result is Result.Success) {
                event(AddSignerSuccessEvent(signerName = signerName, signerSpec = signerSpec))
            } else if (result is Result.Error) {
                event(AddSignerErrorEvent(result.exception.message.orUnknownError()))
            }
        }
    }

    private fun validateInput(signerName: String, signerSpec: String, doAfterValidate: (SignerInput) -> Unit = {}) {
        if (signerName.isEmpty()) {
            event(SignerNameRequiredEvent)
        } else {
            try {
                doAfterValidate(signerSpec.toSigner())
            } catch (e: InvalidSignerFormatException) {
                event(InvalidSignerSpecEvent)
            }
        }
    }
}