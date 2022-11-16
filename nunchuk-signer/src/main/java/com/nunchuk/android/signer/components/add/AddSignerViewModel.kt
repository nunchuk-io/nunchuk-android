/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.signer.components.add

import android.app.Application
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.signer.InvalidSignerFormatException
import com.nunchuk.android.core.signer.SignerInput
import com.nunchuk.android.core.signer.toSigner
import com.nunchuk.android.core.util.getFileFromUri
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.signer.components.add.AddSignerEvent.*
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.CreatePassportSignersUseCase
import com.nunchuk.android.usecase.CreateSignerUseCase
import com.nunchuk.android.usecase.ParseJsonSignerUseCase
import com.nunchuk.android.utils.CrashlyticsReporter
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class AddSignerViewModel @Inject constructor(
    private val createSignerUseCase: CreateSignerUseCase,
    private val createPassportSignersUseCase: CreatePassportSignersUseCase,
    private val parseJsonSignerUseCase: ParseJsonSignerUseCase,
    private val application: Application,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : NunchukViewModel<Unit, AddSignerEvent>() {
    private val qrDataList = HashSet<String>()
    private var isProcessing = false
    override val initialState = Unit

    private val _signers = mutableListOf<SingleSigner>()
    val signers: List<SingleSigner>
        get() = _signers

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
            ).flowOn(IO)
                .onStart { setEvent(LoadingEvent(true)) }
                .onCompletion { setEvent(LoadingEvent(false)) }
                .onException { event(AddSignerErrorEvent(it.message.orUnknownError())) }
                .flowOn(Main)
                .collect { event(AddSignerSuccessEvent(it)) }
        }
    }

    private fun validateInput(
        signerName: String,
        signerSpec: String,
        doAfterValidate: (SignerInput) -> Unit = {}
    ) {
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

    fun handAddPassportSigners(qrData: String) {
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
                        event(ParseKeystoneSignerSuccess(it))
                    }
            }
        }
    }

    fun parseAirgapSigner(uri: Uri) {
        viewModelScope.launch {
            setEvent(LoadingEvent(true))
            withContext(ioDispatcher) {
                getFileFromUri(application.contentResolver, uri, application.cacheDir)?.readText()
            }?.let { content ->
                val result = parseJsonSignerUseCase(
                    ParseJsonSignerUseCase.Params(content, SignerType.AIRGAP)
                )
                if (result.isSuccess) {
                    _signers.apply {
                        clear()
                        addAll(result.getOrThrow())
                    }
                    setEvent(ParseKeystoneSignerSuccess(result.getOrThrow()))
                } else {
                    setEvent(AddSignerErrorEvent(result.exceptionOrNull()?.message.orUnknownError()))
                }
            }
            setEvent(LoadingEvent(false))
        }
    }

    companion object {
        private const val TAG = "AddSignerViewModel"
    }
}