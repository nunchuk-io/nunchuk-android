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
import com.google.gson.Gson
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.GetAppSettingUseCase
import com.nunchuk.android.core.signer.InvalidSignerFormatException
import com.nunchuk.android.core.signer.SignerInput
import com.nunchuk.android.core.signer.toSigner
import com.nunchuk.android.core.util.getFileFromUri
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.model.SignerExtra
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.signer.components.add.AddAirgapSignerEvent.*
import com.nunchuk.android.signer.util.isTestNetPath
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.CreatePassportSignersUseCase
import com.nunchuk.android.usecase.CreateSignerUseCase
import com.nunchuk.android.usecase.ParseJsonSignerUseCase
import com.nunchuk.android.usecase.membership.SaveMembershipStepUseCase
import com.nunchuk.android.utils.CrashlyticsReporter
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class AddAirgapSignerViewModel @Inject constructor(
    private val createSignerUseCase: CreateSignerUseCase,
    private val createPassportSignersUseCase: CreatePassportSignersUseCase,
    private val parseJsonSignerUseCase: ParseJsonSignerUseCase,
    private val application: Application,
    private val saveMembershipStepUseCase: SaveMembershipStepUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val gson: Gson,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val getAppSettingUseCase: GetAppSettingUseCase,
) : NunchukViewModel<Unit, AddAirgapSignerEvent>() {
    private val qrDataList = HashSet<String>()
    private var isProcessing = false
    override val initialState = Unit
    private var chain: Chain = Chain.MAIN

    init {
        viewModelScope.launch {
            chain = getAppSettingUseCase.execute().first().chain
        }
    }

    private val _signers = mutableListOf<SingleSigner>()
    val signers: List<SingleSigner>
        get() = _signers

    fun handleAddAirgapSigner(signerName: String, signerSpec: String, isMembershipFlow: Boolean) {
        val newSignerName = if (isMembershipFlow) "Hardware key${
            membershipStepManager.getNextKeySuffixByType(
                SignerType.AIRGAP
            )
        }" else signerName
        validateInput(newSignerName, signerSpec) {
            doAfterValidate(newSignerName, it, isMembershipFlow)
        }
    }

    private fun doAfterValidate(
        signerName: String,
        signerInput: SignerInput,
        isMembershipFlow: Boolean
    ) {
        viewModelScope.launch {
            if (membershipStepManager.isKeyExisted(signerInput.fingerPrint) && isMembershipFlow) {
                setEvent(AddSameKey)
                return@launch
            }
            setEvent(LoadingEventAirgap(true))
            val result = createSignerUseCase(
                CreateSignerUseCase.Params(
                    name = signerName,
                    xpub = signerInput.xpub,
                    derivationPath = signerInput.derivationPath,
                    masterFingerprint = signerInput.fingerPrint.lowercase(),
                    type = SignerType.AIRGAP
                )
            )
            if (result.isSuccess) {
                val airgap = result.getOrThrow()
                if (isMembershipFlow) {
                    saveMembershipStepUseCase(
                        MembershipStepInfo(
                            step = membershipStepManager.currentStep
                                ?: throw IllegalArgumentException("Current step empty"),
                            masterSignerId = airgap.masterFingerprint,
                            plan = membershipStepManager.plan,
                            verifyType = VerifyType.APP_VERIFIED,
                            extraData = gson.toJson(
                                SignerExtra(
                                    derivationPath = airgap.derivationPath,
                                    isAddNew = true,
                                    signerType = airgap.type
                                )
                            )
                        )
                    )
                }
                setEvent(AddAirgapSignerSuccessEvent(result.getOrThrow()))
            } else {
                setEvent(AddAirgapSignerErrorEvent(result.exceptionOrNull()?.message.orUnknownError()))
            }
            setEvent(LoadingEventAirgap(false))
        }
    }

    private fun validateInput(
        signerName: String,
        signerSpec: String,
        doAfterValidate: (SignerInput) -> Unit = {}
    ) {
        if (signerName.isEmpty()) {
            event(AirgapSignerNameRequiredEvent)
        } else {
            try {
                doAfterValidate(signerSpec.toSigner())
            } catch (e: InvalidSignerFormatException) {
                CrashlyticsReporter.recordException(e)
                event(InvalidAirgapSignerSpecEvent)
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
                        event(ParseKeystoneAirgapSignerSuccess(it))
                    }
            }
        }
    }

    fun parseAirgapSigner(uri: Uri) {
        viewModelScope.launch {
            setEvent(LoadingEventAirgap(true))
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
                    if (chain == Chain.MAIN && _signers.any { isTestNetPath(it.derivationPath) }) {
                        setEvent(ErrorMk4TestNet)
                    } else {
                        setEvent(ParseKeystoneAirgapSignerSuccess(result.getOrThrow()))
                    }
                } else {
                    setEvent(AddAirgapSignerErrorEvent(result.exceptionOrNull()?.message.orUnknownError()))
                }
            }
            setEvent(LoadingEventAirgap(false))
        }
    }

    companion object {
        private const val TAG = "AddSignerViewModel"
    }
}