package com.nunchuk.android.signer.details

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.signer.details.SignerInfoEvent.*
import com.nunchuk.android.signer.util.SignerInput
import com.nunchuk.android.signer.util.toSingleSigner
import com.nunchuk.android.usecase.DeleteRemoteSignerUseCase
import com.nunchuk.android.usecase.UpdateRemoteSignerUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class SignerInfoViewModel @Inject constructor(
    private val deleteRemoteSignerUseCase: DeleteRemoteSignerUseCase,
    private val updateRemoteSignerUseCase: UpdateRemoteSignerUseCase
) : NunchukViewModel<Unit, SignerInfoEvent>() {

    override val initialState = Unit

    lateinit var signerSpec: String

    fun init(signerSpec: String) {
        this.signerSpec = signerSpec
    }

    fun handleEditCompletedEvent(updateSignerName: String) {
        viewModelScope.launch {
            when (val result = updateRemoteSignerUseCase.execute(signer = signerSpec.toSingleSigner(updateSignerName))) {
                is Success -> event(UpdateNameSuccessEvent(updateSignerName))
                is Error -> event(UpdateNameErrorEvent(result.exception.message.orUnknownError()))
            }
        }
    }

    fun handleRemoveSigner(signer: SignerInput) {
        viewModelScope.launch {
            when (val result = deleteRemoteSignerUseCase.execute(
                masterFingerprint = signer.fingerPrint,
                derivationPath = signer.derivationPath
            )) {
                is Success -> event(RemoveSignerCompletedEvent)
                is Error -> event(RemoveSignerErrorEvent(result.exception.message.orUnknownError()))
            }
        }
    }

}