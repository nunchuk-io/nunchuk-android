package com.nunchuk.android.main.components.signer

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.main.components.signer.AddSignerEvent.*
import com.nunchuk.android.main.util.InvalidSignerFormatException
import com.nunchuk.android.main.util.SignerMapper
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.CreateSignerUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class AddSignerViewModel @Inject constructor(
    private val createSignerUseCase: CreateSignerUseCase
) : NunchukViewModel<Unit, AddSignerEvent>() {

    fun handleAddSigner(signerName: String, signerSpec: String) {
        viewModelScope.launch {
            val parseInput = SignerMapper.toSigner(signerSpec)
            val result = createSignerUseCase.execute(
                name = signerName,
                xpub = parseInput.xpub,
                derivationPath = parseInput.path,
                masterFingerprint = parseInput.fingerPrint,
                publicKey = ""
            )
            if (result is Result.Success) {
                event(AddSignerSuccessEvent(signerName = signerName, signerSpec = signerSpec))
            } else if (result is Result.Error) {
                if (result.exception is InvalidSignerFormatException) {
                    event(InvalidSignerSpecEvent)
                } else {
                    event(SignerExistedEvent)
                }
            }
        }
    }

    override val initialState = Unit
}