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

package com.nunchuk.android.signer.mk4.intro

import android.nfc.NdefRecord
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.CreateMk4SignerUseCase
import com.nunchuk.android.core.domain.CreateWallet2UseCase
import com.nunchuk.android.core.domain.GetMk4SingersUseCase
import com.nunchuk.android.core.domain.ImportWalletFromMk4UseCase
import com.nunchuk.android.core.domain.coldcard.ExtractWalletsFromColdCard
import com.nunchuk.android.core.domain.settings.GetChainSettingFlowUseCase
import com.nunchuk.android.core.domain.wallet.ParseMk4WalletUseCase
import com.nunchuk.android.core.helper.CheckAssistedSignerExistenceHelper
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.signer.toSingleSigner
import com.nunchuk.android.core.util.DEFAULT_COLDCARD_WALLET_NAME
import com.nunchuk.android.core.util.gson
import com.nunchuk.android.core.util.isIdentical
import com.nunchuk.android.core.util.isRecommendedMultiSigPath
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.model.SignerExtra
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.toIndex
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.signer.util.isTestNetPath
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.CheckExistingKeyUseCase
import com.nunchuk.android.usecase.GetIndexFromPathUseCase
import com.nunchuk.android.usecase.ResultExistingKey
import com.nunchuk.android.usecase.byzantine.GetReplaceSignerNameUseCase
import com.nunchuk.android.usecase.membership.SaveMembershipStepUseCase
import com.nunchuk.android.usecase.membership.SetKeyVerifiedUseCase
import com.nunchuk.android.usecase.membership.SetReplaceKeyVerifiedUseCase
import com.nunchuk.android.usecase.membership.SyncDraftWalletUseCase
import com.nunchuk.android.usecase.membership.SyncKeyUseCase
import com.nunchuk.android.usecase.replace.ReplaceKeyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class Mk4IntroViewModel @Inject constructor(
    private val getMk4SingersUseCase: GetMk4SingersUseCase,
    private val createMk4SignerUseCase: CreateMk4SignerUseCase,
    private val saveMembershipStepUseCase: SaveMembershipStepUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val getChainSettingFlowUseCase: GetChainSettingFlowUseCase,
    private val importWalletFromMk4UseCase: ImportWalletFromMk4UseCase,
    private val parseMk4WalletUseCase: ParseMk4WalletUseCase,
    private val extractWalletsFromColdCard: ExtractWalletsFromColdCard,
    private val createWallet2UseCase: CreateWallet2UseCase,
    private val syncKeyUseCase: SyncKeyUseCase,
    private val syncDraftWalletUseCase: SyncDraftWalletUseCase,
    private val setKeyVerifiedUseCase: SetKeyVerifiedUseCase,
    private val setReplaceKeyVerifiedUseCase: SetReplaceKeyVerifiedUseCase,
    private val checkAssistedSignerExistenceHelper: CheckAssistedSignerExistenceHelper,
    private val checkExistingKeyUseCase: CheckExistingKeyUseCase,
    private val replaceKeyUseCase: ReplaceKeyUseCase,
    private val getReplaceSignerNameUseCase: GetReplaceSignerNameUseCase,
    private val pushEventManager: PushEventManager,
    private val getIndexFromPathUseCase: GetIndexFromPathUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _event = MutableSharedFlow<Mk4IntroViewEvent>()
    private val args: Mk4IntroFragmentArgs =
        Mk4IntroFragmentArgs.fromSavedStateHandle(savedStateHandle)
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(Mk4IntroState())
    val state = _state.asStateFlow()

    val remainTime = membershipStepManager.remainingTime
    private var chain: Chain = Chain.MAIN

    private val _mk4Signers = mutableListOf<SingleSigner>()

    init {
        viewModelScope.launch {
            chain = getChainSettingFlowUseCase(Unit).map { it.getOrElse { Chain.MAIN } }.first()
        }
        checkAssistedSignerExistenceHelper.init(viewModelScope)
    }

    val mk4Signers: List<SingleSigner>
        get() = _mk4Signers

    fun getMk4Signer(
        records: List<NdefRecord>,
        groupId: String,
        newIndex: Int,
        xfp: String?,
        replacedXfp: String?,
        walletId: String?,
        onChainAddSignerParam: OnChainAddSignerParam? = null
    ) {
        viewModelScope.launch {
            _event.emit(Mk4IntroViewEvent.Loading(true))
            val result = getMk4SingersUseCase(records.toTypedArray())
            if (result.isSuccess) {
                if (args.isMembershipFlow) {
                    val sortedSigner = result.getOrThrow().sortedBy { it.derivationPath }
                    val signer = sortedSigner.find { it.derivationPath.isRecommendedMultiSigPath }
                    if (signer == null) {
                        _event.emit(Mk4IntroViewEvent.ShowError("XPUBs file is invalid"))
                        _event.emit(Mk4IntroViewEvent.Loading(false))
                        return@launch
                    }
                    if (chain == Chain.MAIN && isTestNetPath(signer.derivationPath)) {
                        _event.emit(Mk4IntroViewEvent.ErrorMk4TestNet)
                        _event.emit(Mk4IntroViewEvent.Loading(false))
                        return@launch
                    }
                    if (!xfp.isNullOrEmpty() && signer.masterFingerprint != xfp) {
                        _event.emit(Mk4IntroViewEvent.XfpNotMatchException)
                        _event.emit(Mk4IntroViewEvent.Loading(false))
                        return@launch
                    }
                    if (newIndex >= 0 && !signer.derivationPath.endsWith("${newIndex}h/2h")) {
                        _event.emit(Mk4IntroViewEvent.NewIndexNotMatchException)
                        _event.emit(Mk4IntroViewEvent.Loading(false))
                        return@launch
                    }
                    if (onChainAddSignerParam != null && signer.masterFingerprint != onChainAddSignerParam.currentSigner?.fingerPrint && onChainAddSignerParam.keyIndex > 0) {
                        _event.emit(
                            Mk4IntroViewEvent.ShowError(
                                "The added key has an XFP mismatch. Please use the same device for both keys."
                            )
                        )
                        _event.emit(Mk4IntroViewEvent.Loading(false))
                        return@launch
                    }
                    if (onChainAddSignerParam?.isVerifyBackupSeedPhrase() == true) {
                        val currentSigner = onChainAddSignerParam.currentSigner
                        if (currentSigner != null) {
                            if (signer.masterFingerprint != currentSigner.fingerPrint) {
                                _event.emit(
                                    Mk4IntroViewEvent.ShowError(
                                        "The key you just added (XFP:${signer.masterFingerprint.uppercase()}) doesn't match the original inheritance key (XFP:${currentSigner.fingerPrint.uppercase()}). Please try again."
                                    )
                                )
                                _event.emit(Mk4IntroViewEvent.Loading(false))
                                return@launch
                            }
                            
                            val newAccountIndex = getIndexFromPathUseCase(signer.derivationPath).getOrElse { 0 }
                            
                            if (newAccountIndex != 0) {
                                _event.emit(
                                    Mk4IntroViewEvent.ShowError(
                                        "The key you just added (XFP:${signer.masterFingerprint.uppercase()}, Account $newAccountIndex) doesn't match the original inheritance key (XFP:${currentSigner.fingerPrint.uppercase()}, Account 0). Please try again."
                                    )
                                )
                                _event.emit(Mk4IntroViewEvent.Loading(false))
                                return@launch
                            }
                        }
                        _event.emit(Mk4IntroViewEvent.OnCreateSignerSuccess(signer))
                        _event.emit(Mk4IntroViewEvent.Loading(false))
                        return@launch
                    }
                    if (membershipStepManager.isKeyExisted(signer.masterFingerprint)) {
                        if (onChainAddSignerParam != null && onChainAddSignerParam.currentSigner != null) {
                            if (signer.isIdentical(onChainAddSignerParam.currentSigner!!.toSingleSigner())) {
                                _event.emit(Mk4IntroViewEvent.OnSignerExistInAssistedWallet)
                                _event.emit(Mk4IntroViewEvent.Loading(false))
                                return@launch
                            }
                        } else {
                            _event.emit(Mk4IntroViewEvent.OnSignerExistInAssistedWallet)
                            _event.emit(Mk4IntroViewEvent.Loading(false))
                            return@launch
                        }
                    }
                    val signerName = if (replacedXfp.isNullOrEmpty()) {
                        membershipStepManager.getInheritanceKeyName(false)

                    } else {
                        getReplaceSignerNameUseCase(
                            GetReplaceSignerNameUseCase.Params(
                                walletId = walletId.orEmpty(),
                                signerType = SignerType.COLDCARD_NFC
                            )
                        ).getOrThrow()
                    }
                    val createSignerResult = createMk4SignerUseCase(
                        signer.copy(
                            name = signerName
                        )
                    )
                    val coldcardSigner = createSignerResult.getOrThrow()
                    if (createSignerResult.isSuccess) {
                        // force type coldcard nfc in case we import hardware key first
                        if (onChainAddSignerParam?.isClaiming == true) {
                            pushEventManager.push(PushEvent.ClaimSignerAdded(signer))
                        } else if (replacedXfp.isNullOrEmpty()) {
                            val coldcardSigner =
                                createSignerResult.getOrThrow()
                                    .copy(type = SignerType.COLDCARD_NFC)
                            val walletType = syncDraftWalletUseCase(groupId).getOrNull()?.walletType ?: WalletType.MULTI_SIG
                            syncKeyUseCase(
                                SyncKeyUseCase.Param(
                                    step = membershipStepManager.currentStep
                                        ?: throw IllegalArgumentException("Current step empty"),
                                    groupId = groupId,
                                    signer = coldcardSigner,
                                    walletType = walletType
                                )
                            ).onSuccess {
                                saveMembershipStepUseCase(
                                    MembershipStepInfo(
                                        step = membershipStepManager.currentStep
                                            ?: throw IllegalArgumentException("Current step empty"),
                                        masterSignerId = coldcardSigner.masterFingerprint,
                                        plan = membershipStepManager.localMembershipPlan,
                                        verifyType = if (args.isAddInheritanceKey.not()) VerifyType.APP_VERIFIED else VerifyType.NONE,
                                        extraData = gson.toJson(
                                            SignerExtra(
                                                derivationPath = coldcardSigner.derivationPath,
                                                isAddNew = true,
                                                signerType = coldcardSigner.type,
                                                userKeyFileName = ""
                                            )
                                        ),
                                        groupId = groupId
                                    )
                                )
                            }
                                .onFailure {
                                _event.emit(Mk4IntroViewEvent.ShowError(it.message.orUnknownError()))
                                return@launch
                            }
                        } else {
                            if (args.isAddInheritanceKey.not()) {
                                replaceKeyUseCase(
                                    ReplaceKeyUseCase.Param(
                                        groupId = groupId,
                                        walletId = walletId.orEmpty(),
                                        xfp = replacedXfp,
                                        signer = createSignerResult.getOrThrow(),
                                        keyIndex = onChainAddSignerParam?.replaceInfo?.step?.toIndex(groupId.isNotEmpty())
                                    )
                                ).onFailure {
                                    _event.emit(Mk4IntroViewEvent.ShowError(it.message.orUnknownError()))
                                    return@launch
                                }
                            }
                        }
                        _event.emit(Mk4IntroViewEvent.OnCreateSignerSuccess(coldcardSigner))
                    } else {
                        _event.emit(Mk4IntroViewEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
                    }
                } else {
                    _mk4Signers.apply {
                        clear()
                        addAll(result.getOrThrow())
                    }
                    _event.emit(Mk4IntroViewEvent.LoadMk4SignersSuccess(_mk4Signers))
                }
            } else {
                _event.emit(Mk4IntroViewEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
            _event.emit(Mk4IntroViewEvent.Loading(false))
        }
    }

    fun checkExistingKey(signer: SingleSigner) {
        _state.update { it.copy(signer = signer) }
        viewModelScope.launch {
            if (checkAssistedSignerExistenceHelper.isInAssistedWallet(signer.toModel())) {
                checkExistingKeyUseCase(CheckExistingKeyUseCase.Params(signer))
                    .onSuccess {
                        _event.emit(Mk4IntroViewEvent.CheckExistingKey(it, signer))
                    }
                    .onFailure {
                        _event.emit(Mk4IntroViewEvent.ShowError(it.message.orUnknownError()))
                    }
            } else {
                _event.emit(Mk4IntroViewEvent.CheckExistingKey(ResultExistingKey.None, signer))
            }
        }
    }

    fun importWalletFromMk4(records: List<NdefRecord>) {
        viewModelScope.launch {
            _event.emit(Mk4IntroViewEvent.NfcLoading(true))
            val result = importWalletFromMk4UseCase(records)
            _event.emit(Mk4IntroViewEvent.NfcLoading(false))
            if (result.isSuccess && result.getOrThrow() != null) {
                _event.emit(Mk4IntroViewEvent.ImportWalletFromMk4Success(result.getOrThrow()!!.id))
            } else {
                _event.emit(Mk4IntroViewEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun parseWalletFromMk4(records: List<NdefRecord>) {
        viewModelScope.launch {
            _event.emit(Mk4IntroViewEvent.NfcLoading(true))
            parseMk4WalletUseCase(records).onSuccess {
                _event.emit(Mk4IntroViewEvent.ParseWalletFromMk4Success(it))
            }.onFailure {
                _event.emit(Mk4IntroViewEvent.ShowError(it.message.orUnknownError()))
            }
            _event.emit(Mk4IntroViewEvent.NfcLoading(false))
        }
    }

    fun getWalletsFromColdCard(records: List<NdefRecord>) {
        viewModelScope.launch {
            _event.emit(Mk4IntroViewEvent.NfcLoading(true))
            val result = extractWalletsFromColdCard(records)
            _event.emit(Mk4IntroViewEvent.NfcLoading(false))
            if (result.isSuccess && result.getOrThrow().isNotEmpty()) {
                _state.update { it.copy(wallets = result.getOrThrow()) }
                _event.emit(Mk4IntroViewEvent.ExtractWalletsFromColdCard(result.getOrThrow()))
            } else {
                _event.emit(Mk4IntroViewEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun createWallet(walletId: String) {
        viewModelScope.launch {
            _state.value.wallets.find { it.id == walletId }?.let { wallet ->
                _event.emit(Mk4IntroViewEvent.Loading(true))
                val result = createWallet2UseCase(wallet.copy(name = DEFAULT_COLDCARD_WALLET_NAME))
                _event.emit(Mk4IntroViewEvent.Loading(false))
                if (result.isSuccess) {
                    _event.emit(Mk4IntroViewEvent.ImportWalletFromMk4Success(result.getOrThrow().id))
                } else {
                    _event.emit(Mk4IntroViewEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
                }
            }
        }
    }

    fun setKeyVerified(groupId: String, masterSignerId: String) {
        viewModelScope.launch {
            setKeyVerifiedUseCase(
                SetKeyVerifiedUseCase.Param(
                    groupId = groupId,
                    masterSignerId = masterSignerId,
                    verifyType = VerifyType.APP_VERIFIED
                )
            ).onSuccess {
                _event.emit(Mk4IntroViewEvent.KeyVerifiedSuccess)
            }.onFailure {
                _event.emit(Mk4IntroViewEvent.ShowError(it.message.orUnknownError()))
            }
        }
    }

    fun setReplaceKeyVerified(keyId: String, groupId: String, walletId: String) {
        viewModelScope.launch {
            setReplaceKeyVerifiedUseCase(
                SetReplaceKeyVerifiedUseCase.Param(
                    keyId = keyId,
                    checkSum = "",
                    verifyType = VerifyType.SELF_VERIFIED,
                    groupId = groupId,
                    walletId = walletId
                )
            ).onSuccess {
                _event.emit(Mk4IntroViewEvent.KeyVerifiedSuccess)
            }.onFailure {
                _event.emit(Mk4IntroViewEvent.ShowError(it.message.orUnknownError()))
            }
        }
    }

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(Mk4IntroViewEvent.OnContinueClicked)
        }
    }
}

data class Mk4IntroState(val wallets: List<Wallet> = emptyList(), val signer: SingleSigner? = null)