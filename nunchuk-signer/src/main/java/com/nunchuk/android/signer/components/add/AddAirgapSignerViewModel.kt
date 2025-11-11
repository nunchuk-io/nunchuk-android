/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
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
import com.nunchuk.android.core.domain.settings.GetChainSettingFlowUseCase
import com.nunchuk.android.core.helper.CheckAssistedSignerExistenceHelper
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.signer.InvalidSignerFormatException
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.core.signer.SignerInput
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.signer.toSigner
import com.nunchuk.android.core.signer.toSingleSigner
import com.nunchuk.android.core.util.formattedName
import com.nunchuk.android.core.util.getFileFromUri
import com.nunchuk.android.core.util.isIdentical
import com.nunchuk.android.core.util.isValidPathForAssistedWallet
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.model.SignerExtra
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.signer.components.add.AddAirgapSignerEvent.AddAirgapSignerErrorEvent
import com.nunchuk.android.signer.components.add.AddAirgapSignerEvent.AddAirgapSignerSuccessEvent
import com.nunchuk.android.signer.components.add.AddAirgapSignerEvent.AddSameKey
import com.nunchuk.android.signer.components.add.AddAirgapSignerEvent.ErrorMk4TestNet
import com.nunchuk.android.signer.components.add.AddAirgapSignerEvent.KeyVerifiedSuccess
import com.nunchuk.android.signer.components.add.AddAirgapSignerEvent.LoadingEventAirgap
import com.nunchuk.android.signer.components.add.AddAirgapSignerEvent.ParseKeystoneAirgapSignerSuccess
import com.nunchuk.android.signer.util.isTestNetPath
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.ChangeKeyTypeUseCase
import com.nunchuk.android.usecase.CheckExistingKeyUseCase
import com.nunchuk.android.usecase.CreatePassportSignersUseCase
import com.nunchuk.android.usecase.CreateSignerUseCase
import com.nunchuk.android.usecase.GetIndexFromPathUseCase
import com.nunchuk.android.usecase.ParseJsonSignerUseCase
import com.nunchuk.android.usecase.ResultExistingKey
import com.nunchuk.android.usecase.byzantine.GetReplaceSignerNameUseCase
import com.nunchuk.android.usecase.membership.SaveMembershipStepUseCase
import com.nunchuk.android.usecase.membership.SetKeyVerifiedUseCase
import com.nunchuk.android.usecase.membership.SyncKeyUseCase
import com.nunchuk.android.usecase.qr.AnalyzeQrUseCase
import com.nunchuk.android.usecase.replace.ReplaceKeyUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import com.nunchuk.android.utils.CrashlyticsReporter
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
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
    private val getChainSettingFlowUseCase: GetChainSettingFlowUseCase,
    private val analyzeQrUseCase: AnalyzeQrUseCase,
    private val syncKeyUseCase: SyncKeyUseCase,
    private val setKeyVerifiedUseCase: SetKeyVerifiedUseCase,
    private val checkExistingKeyUseCase: CheckExistingKeyUseCase,
    private val checkAssistedSignerExistenceHelper: CheckAssistedSignerExistenceHelper,
    private val changeKeyTypeUseCase: ChangeKeyTypeUseCase,
    private val replaceKeyUseCase: ReplaceKeyUseCase,
    private val getReplaceSignerNameUseCase: GetReplaceSignerNameUseCase,
    private val pushEventManager: PushEventManager,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val getIndexFromPathUseCase: GetIndexFromPathUseCase,
) : NunchukViewModel<Unit, AddAirgapSignerEvent>() {
    private val qrDataList = HashSet<String>()
    private var isProcessing = false
    override val initialState = Unit
    var chain: Chain = Chain.MAIN
    private var groupId: String = ""
    private var isMembershipFlow = false
    private var replacedXfp: String? = null
    private var walletId: String = ""
    private var walletType = WalletType.MULTI_SIG
    private var onChainAddSignerParam: OnChainAddSignerParam? = null

    private val _state = MutableStateFlow(AddAirgapSignerState())
    val uiState = _state.asStateFlow()

    init {
        viewModelScope.launch {
            chain = getChainSettingFlowUseCase(Unit).map { it.getOrElse { Chain.MAIN } }.first()
        }
        checkAssistedSignerExistenceHelper.init(viewModelScope)
    }

    val remainTime = membershipStepManager.remainingTime

    fun init(groupId: String, isMembershipFlow: Boolean, replacedXfp: String?, walletId: String, onChainAddSignerParam: com.nunchuk.android.core.signer.OnChainAddSignerParam? = null) {
        this.groupId = groupId
        this.isMembershipFlow = isMembershipFlow
        this.replacedXfp = replacedXfp
        this.walletId = walletId
        this.onChainAddSignerParam = onChainAddSignerParam
        if (onChainAddSignerParam != null) {
            this.walletType = WalletType.MINISCRIPT
        } else if (walletId.isNotEmpty()) {
            getWalletType(walletId)
        }
    }

    private val _signers = mutableListOf<SingleSigner>()
    val signers: List<SingleSigner>
        get() = _signers

    private fun getWalletType(walletId: String) = viewModelScope.launch {
        getWalletDetail2UseCase(walletId)
            .onSuccess { wallet ->
                walletType = if (wallet.signers.size > 1) {
                    WalletType.MULTI_SIG
                } else {
                    WalletType.SINGLE_SIG
                }
            }
    }

    fun handleAddAirgapSigner(
        signerName: String,
        signerSpec: String,
        isMembershipFlow: Boolean,
        signerTag: SignerTag?,
        xfp: String?,
        newIndex: Int,
    ) {
        validateInput(signerSpec) {
            doAfterValidate(
                signerName = signerName,
                signerInput = it,
                isMembershipFlow = isMembershipFlow,
                signerTag = signerTag,
                xfp = xfp,
                newIndex = newIndex
            )
        }
    }

    private fun doAfterValidate(
        signerName: String,
        signerInput: SignerInput,
        isMembershipFlow: Boolean,
        signerTag: SignerTag?,
        xfp: String?,
        newIndex: Int,
    ) {
        viewModelScope.launch {
            val newSignerName =
                if (isMembershipFlow && !replacedXfp.isNullOrEmpty() && walletId.isNotEmpty()) {
                    getReplaceSignerNameUseCase(
                        GetReplaceSignerNameUseCase.Params(
                            walletId = walletId,
                            signerType = SignerType.AIRGAP
                        )
                    ).getOrThrow()
                } else if (isMembershipFlow) {
                    "${signerTag.formattedName}${
                        membershipStepManager.getNextKeySuffixByType(
                            SignerType.AIRGAP
                        )
                    }"
                } else {
                    signerName
                }
            if (newSignerName.isEmpty()) {
                _state.update { it.copy(showKeyNameError = true) }
                return@launch
            }
            if (onChainAddSignerParam != null ) {
                val signer = signerInput.toSingleSigner(newSignerName, signerTag)
                if (onChainAddSignerParam?.isVerifyBackupSeedPhrase() == true) {
                    val currentSigner = onChainAddSignerParam!!.currentSigner
                    if (currentSigner != null) {
                        if (signer.masterFingerprint != currentSigner.fingerPrint) {
                            setEvent(
                                AddAirgapSignerErrorEvent(
                                    "The key you just added (XFP:${signer.masterFingerprint.uppercase()}) doesn't match the original inheritance key (XFP:${currentSigner.fingerPrint.uppercase()}). Please try again."
                                )
                            )
                            setEvent(LoadingEventAirgap(false))
                            return@launch
                        }
                        
                        val newAccountIndex = getIndexFromPathUseCase(signer.derivationPath).getOrElse { 0 }
                        
                        if (newAccountIndex != 0) {
                            setEvent(
                                AddAirgapSignerErrorEvent(
                                    "The key you just added (XFP:${signer.masterFingerprint.uppercase()}, Account $newAccountIndex) doesn't match the original inheritance key (XFP:${currentSigner.fingerPrint.uppercase()}, Account 0). Please try again."
                                )
                            )
                            setEvent(LoadingEventAirgap(false))
                            return@launch
                        }
                    }
                    setEvent(AddAirgapSignerSuccessEvent(signer))
                    setEvent(LoadingEventAirgap(false))
                    return@launch
                } else if (onChainAddSignerParam != null && signer.masterFingerprint != onChainAddSignerParam!!.currentSigner?.fingerPrint && onChainAddSignerParam!!.keyIndex > 0){
                    setEvent(
                        AddAirgapSignerErrorEvent(
                            "The added key has an XFP mismatch. Please use the same device for both keys."
                        )
                    )
                    setEvent(LoadingEventAirgap(false))
                }
            }

            if (replacedXfp.isNullOrEmpty() && membershipStepManager.isKeyExisted(signerInput.fingerPrint) && isMembershipFlow) {
                if (onChainAddSignerParam != null && onChainAddSignerParam?.currentSigner != null) {
                    val signer = signerInput.toSingleSigner(newSignerName, signerTag)
                    if (signer.isIdentical(onChainAddSignerParam?.currentSigner!!.toSingleSigner())) {
                        setEvent(AddSameKey)
                        setEvent(LoadingEventAirgap(false))
                        return@launch
                    }
                } else {
                    setEvent(AddSameKey)
                    setEvent(LoadingEventAirgap(false))
                    return@launch
                }
            }
            if (!xfp.isNullOrEmpty() && signerInput.fingerPrint != xfp) {
                setEvent(AddAirgapSignerEvent.XfpNotMatchException)
                return@launch
            }
            val formatedDerivationPath = signerInput.derivationPath
                .replace("'", "h")
            if (newIndex >= 0 && walletType == WalletType.MULTI_SIG && !formatedDerivationPath.endsWith("${newIndex}h/2h")) {
                setEvent(AddAirgapSignerEvent.NewIndexNotMatchException)
                return@launch
            }
            if (newIndex >= 0 && walletType == WalletType.SINGLE_SIG && !formatedDerivationPath.endsWith("${newIndex}h")) {
                setEvent(AddAirgapSignerEvent.NewIndexNotMatchException)
                return@launch
            }
            setEvent(LoadingEventAirgap(true))
            val signer = signerInput.toSingleSigner(newSignerName, signerTag)
            if (isMembershipFlow.not() && checkAssistedSignerExistenceHelper.isInAssistedWallet(
                    signer.toModel()
                )
            ) {
                _state.update { it.copy(airgap = signer.copy(derivationPath = signerInput.derivationPath)) }
                val resultKey = checkExistingKeyUseCase(CheckExistingKeyUseCase.Params(signer))
                setEvent(LoadingEventAirgap(false))
                if (resultKey.isSuccess) {
                    resultKey.getOrNull()?.let {
                        if (it != ResultExistingKey.None) {
                            setEvent(AddAirgapSignerEvent.CheckExisting(it, signer))
                            return@launch
                        }
                    }
                }
            }
            val result = createSignerUseCase(
                CreateSignerUseCase.Params(
                    name = newSignerName,
                    xpub = signerInput.xpub,
                    derivationPath = signerInput.derivationPath,
                    masterFingerprint = signerInput.fingerPrint.lowercase(),
                    type = SignerType.AIRGAP,
                    tags = signerTag?.let { listOf(it) }.orEmpty()
                )
            )
            if (result.isSuccess) {
                val airgap = result.getOrThrow()
                pushEventManager.push(PushEvent.LocalUserSignerAdded(airgap))
                if (isMembershipFlow) {
                    if (onChainAddSignerParam?.isClaiming == true) {
                        pushEventManager.push(
                            PushEvent.LocalUserSignerAdded(
                                airgap
                            )
                        )
                    } else if (walletId.isNotEmpty() && !replacedXfp.isNullOrEmpty()) {
                        replaceKeyUseCase(
                            ReplaceKeyUseCase.Param(
                                groupId = groupId,
                                walletId = walletId,
                                xfp = replacedXfp.orEmpty(),
                                signer = airgap
                            )
                        ).onFailure {
                            setEvent(AddAirgapSignerErrorEvent(it.message.orUnknownError()))
                            setEvent(LoadingEventAirgap(false))
                            return@launch
                        }
                    } else {
                        syncKeyUseCase(
                            SyncKeyUseCase.Param(
                                step = membershipStepManager.currentStep
                                    ?: throw IllegalArgumentException("Current step empty"),
                                groupId = groupId,
                                signer = airgap,
                                walletType = walletType
                            )
                        ).onSuccess {
                            saveMembershipStepUseCase(
                                MembershipStepInfo(
                                    step = membershipStepManager.currentStep
                                        ?: throw IllegalArgumentException("Current step empty"),
                                    masterSignerId = airgap.masterFingerprint,
                                    plan = membershipStepManager.localMembershipPlan,
                                    verifyType = VerifyType.APP_VERIFIED,
                                    extraData = gson.toJson(
                                        SignerExtra(
                                            derivationPath = airgap.derivationPath,
                                            isAddNew = true,
                                            signerType = airgap.type,
                                            userKeyFileName = ""
                                        )
                                    ),
                                    groupId = groupId
                                )
                            )
                        }.onFailure {
                            setEvent(AddAirgapSignerErrorEvent(it.message.orUnknownError()))
                            setEvent(LoadingEventAirgap(false))
                            return@launch
                        }
                    }
                    setEvent(AddAirgapSignerSuccessEvent(result.getOrThrow()))
                } else {
                    setEvent(AddAirgapSignerSuccessEvent(result.getOrThrow()))
                }
            } else {
                setEvent(AddAirgapSignerErrorEvent(result.exceptionOrNull()?.message.orUnknownError()))
            }
            setEvent(LoadingEventAirgap(false))
        }
    }

    private fun validateInput(
        signerSpec: String,
        doAfterValidate: (SignerInput) -> Unit = {},
    ) {
        try {
            doAfterValidate(signerSpec.toSigner())
        } catch (e: InvalidSignerFormatException) {
            CrashlyticsReporter.recordException(e)
            _state.update { it.copy(showKeySpecError = true) }
        }
    }

    fun handAddPassportSigners(qrData: String) {
        qrDataList.add(qrData)
        if (!isProcessing) {
            analyzeQr()
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

    private fun analyzeQr() {
        viewModelScope.launch {
            val result = analyzeQrUseCase(qrDataList.toList())
            if (result.isSuccess) {
                Timber.d("analyzeQrUseCase: ${result.getOrThrow()}")
                _state.update { it.copy(progress = result.getOrThrow().times(100.0)) }
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
                    val signers = result.getOrThrow()
                    validateAndUpdateSigners(signers)
                    if (isMembershipFlow && chain == Chain.MAIN && _signers.any { isTestNetPath(it.derivationPath) }) {
                        setEvent(ErrorMk4TestNet)
                    } else {
                        setEvent(ParseKeystoneAirgapSignerSuccess(_signers))
                    }
                } else {
                    setEvent(AddAirgapSignerErrorEvent("XPUBs file is invalid"))
                }
            }
            setEvent(LoadingEventAirgap(false))
        }
    }

    fun validateAndUpdateSigners(originalSigners: List<SingleSigner>): List<SingleSigner> {
        _signers.apply {
            clear()
            if (isMembershipFlow) {
                addAll(originalSigners.filter { it.derivationPath.isValidPathForAssistedWallet })
            } else {
                addAll(originalSigners)
            }
        }
        if (originalSigners.isNotEmpty() && _signers.isEmpty()) {
            setEvent(AddAirgapSignerErrorEvent("Single-sig key detected. Please add a BIP48 multisig key."))
        }
        return _signers
    }

    fun updateKeyName(keyName: String) {
        _state.update { it.copy(keyName = keyName, showKeyNameError = false) }
    }

    fun updateKeySpec(keySpec: String) {
        _state.update { it.copy(keySpec = keySpec, showKeySpecError = false) }
    }

    fun changeKeyType(
        signerTag: SignerTag? = null,
    ) {
        val singleSigner = _state.value.airgap ?: return
        viewModelScope.launch {
            changeKeyTypeUseCase(
                ChangeKeyTypeUseCase.Params(
                    singleSigner = singleSigner.copy(
                        type = SignerType.AIRGAP,
                        tags = listOfNotNull(signerTag)
                    )
                )
            ).onSuccess {
                setEvent(AddAirgapSignerSuccessEvent(it))
            }.onFailure {
                setEvent(AddAirgapSignerErrorEvent(it.message.orUnknownError()))
            }
        }
    }
    
    fun setKeyVerified(groupId: String, masterSignerId: String) {
        viewModelScope.launch {
            setKeyVerifiedUseCase(
                SetKeyVerifiedUseCase.Param(
                    groupId = groupId,
                    masterSignerId = masterSignerId,
                    isAppVerified = true
                )
            ).onSuccess {
                setEvent(KeyVerifiedSuccess)
            }.onFailure {
                setEvent(AddAirgapSignerErrorEvent(it.message.orUnknownError()))
            }
        }
    }

    companion object {
        private const val TAG = "AddSignerViewModel"
    }
}

data class AddAirgapSignerState(
    val progress: Double = 0.0,
    val keySpec: String = "",
    val keyName: String = "",
    val showKeySpecError: Boolean = false,
    val showKeyNameError: Boolean = false,
    val airgap: SingleSigner? = null,
    val signerInput: SignerInput? = null,
)

internal fun SignerInput.toSingleSigner(name: String, tag: SignerTag?): SingleSigner {
    return SingleSigner(
        name = name,
        xpub = xpub,
        derivationPath = derivationPath.replace("'", "h"),
        masterFingerprint = fingerPrint,
        type = SignerType.AIRGAP,
        tags = listOfNotNull(tag)
    )
}