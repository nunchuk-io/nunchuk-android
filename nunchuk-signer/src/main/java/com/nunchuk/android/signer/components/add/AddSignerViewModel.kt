package com.nunchuk.android.signer.components.add

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.signer.InvalidSignerFormatException
import com.nunchuk.android.core.signer.SignerInput
import com.nunchuk.android.core.signer.toSigner
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.signer.components.add.AddSignerEvent.*
import com.nunchuk.android.usecase.CreateCoboSignerUseCase
import com.nunchuk.android.usecase.CreateSignerUseCase
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

internal class AddSignerViewModel @Inject constructor(
    private val createSignerUseCase: CreateSignerUseCase,
    private val createCoboSignerUseCase: CreateCoboSignerUseCase
) : NunchukViewModel<Unit, AddSignerEvent>() {

    override val initialState = Unit

    fun handleAddSigner(signerName: String, signerSpec: String) {
        validateInput(signerName, signerSpec) {
            doAfterValidate(signerName, it)
        }
    }

    private fun doAfterValidate(signerName: String, signerInput: SignerInput) {
        viewModelScope.launch {
            event(LoadingEvent)
            val result = createSignerUseCase.execute(
                name = signerName,
                xpub = signerInput.xpub,
                derivationPath = signerInput.derivationPath,
                masterFingerprint = signerInput.fingerPrint.toLowerCase(Locale.getDefault()),
                publicKey = ""
            )
            when (result) {
                is Success -> event(AddSignerSuccessEvent(id = result.data.masterSignerId, name = result.data.name))
                is Error -> event(AddSignerErrorEvent(result.exception.message.orUnknownError()))
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

    fun handleAddCoboSigner(signerName: String, jsonInfo: String) {
        if (signerName.isEmpty()) {
            event(SignerNameRequiredEvent)
        } else {
            viewModelScope.launch {
                event(LoadingEvent)
                val result = createCoboSignerUseCase.execute(
                    name = signerName,
                    jsonInfo = jsonInfo
                )
                when (result) {
                    is Success -> event(AddSignerSuccessEvent(id = result.data.masterSignerId, name = result.data.name))
                    is Error -> event(AddSignerErrorEvent(result.exception.message.orUnknownError()))
                }
            }
        }
    }
}