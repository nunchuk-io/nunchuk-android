package com.nunchuk.android.signer.components.add

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.signer.InvalidSignerFormatException
import com.nunchuk.android.core.signer.SignerInput
import com.nunchuk.android.core.signer.toSigner
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.toSpec
import com.nunchuk.android.signer.components.add.AddSignerEvent.*
import com.nunchuk.android.usecase.CreateKeystoneSignerUseCase
import com.nunchuk.android.usecase.CreatePassportSignersUseCase
import com.nunchuk.android.usecase.CreateSignerUseCase
import com.nunchuk.android.utils.CrashlyticsReporter
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class AddSignerViewModel @Inject constructor(
    private val createSignerUseCase: CreateSignerUseCase,
    private val createKeystoneSignerUseCase: CreateKeystoneSignerUseCase,
    private val createPassportSignersUseCase: CreatePassportSignersUseCase
) : NunchukViewModel<Unit, AddSignerEvent>() {

    private val qrDataList = HashSet<String>()
    private var isProcessing = false
    override val initialState = Unit

    fun handleAddSigner(signerName: String, signerSpec: String) {
        validateInput(signerName, signerSpec) {
            doAfterValidate(signerName, it)
        }
    }

    private fun doAfterValidate(signerName: String, signerInput: SignerInput) {
        viewModelScope.launch {
            createSignerUseCase.execute(
                name = signerName,
                xpub = signerInput.xpub,
                derivationPath = signerInput.derivationPath,
                masterFingerprint = signerInput.fingerPrint.lowercase(),
                publicKey = ""
            )
                .onStart { event(LoadingEvent) }
                .flowOn(IO)
                .onException { event(AddSignerErrorEvent(it.message.orUnknownError())) }
                .flowOn(Main)
                .collect { event(AddSignerSuccessEvent(id = it.masterSignerId, name = it.name)) }
        }
    }

    private fun validateInput(signerName: String, signerSpec: String, doAfterValidate: (SignerInput) -> Unit = {}) {
        if (signerName.isEmpty()) {
            event(SignerNameRequiredEvent)
        } else {
            try {
                doAfterValidate(signerSpec.toSigner())
            } catch (e: InvalidSignerFormatException) {
                CrashlyticsReporter.recordException(e)
                event(InvalidSignerSpecEvent)
            }
        }
    }

    fun handleAddQrData(qrData: String) {
        viewModelScope.launch {
            createKeystoneSignerUseCase.execute(qrData = qrData)
                .onStart { event(LoadingEvent) }
                .flowOn(IO)
                .onException { event(AddSignerErrorEvent(it.message.orUnknownError())) }
                .flowOn(Main)
                .collect { event(ParseKeystoneSignerSuccess(it.toSpec())) }
        }
    }

    fun handAddPassportSigners(qrData: String, onSuccessEvent: (List<SingleSigner>) -> Unit = {}) {
        qrDataList.add(qrData)
        if (!isProcessing) {
            viewModelScope.launch {
                Timber.tag(TAG).d("qrDataList::${qrDataList.size}")
                createPassportSignersUseCase.execute(qrData = qrDataList.toList())
                    .onStart { isProcessing = true }
                    .flowOn(IO)
                    .onException { }
                    .flowOn(Main)
                    .onCompletion { isProcessing = false }
                    .collect {
                        Timber.tag(TAG).d("add passport signer successful::$it")
                        onSuccessEvent(it)
                    }
            }
        }
    }

    companion object {
        private const val TAG = "AddSignerViewModel"
    }
}