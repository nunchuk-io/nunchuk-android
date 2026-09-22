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

package com.nunchuk.android.main.membership.key

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.nunchuk.android.core.domain.utils.NfcFileManager
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.isRecommendedMultiSigPath
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.main.membership.model.AddKeyData
import com.nunchuk.android.main.membership.model.encryptedBackupClaimOption
import com.nunchuk.android.main.membership.model.owesEncryptedBackup
import com.nunchuk.android.main.membership.model.toGroupWalletType
import com.nunchuk.android.main.membership.model.toSteps
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.model.SignerExtra
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.signer.SignerServer
import com.nunchuk.android.model.inheritance.ClaimOption
import com.nunchuk.android.model.isAddInheritanceKey
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.GetIndexFromPathUseCase
import com.nunchuk.android.usecase.UpdateRemoteSignerUseCase
import com.nunchuk.android.usecase.membership.CheckRequestAddDesktopKeyStatusUseCase
import com.nunchuk.android.usecase.membership.GetMembershipStepUseCase
import com.nunchuk.android.usecase.membership.SaveMembershipStepUseCase
import com.nunchuk.android.usecase.membership.SetKeyVerifiedUseCase
import com.nunchuk.android.usecase.membership.SyncDraftWalletUseCase
import com.nunchuk.android.usecase.membership.SyncKeyUseCase
import com.nunchuk.android.usecase.signer.GetAllSignersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddKeyListViewModel @Inject constructor(
    private val getAllSignersUseCase: GetAllSignersUseCase,
    private val savedStateHandle: SavedStateHandle,
    getMembershipStepUseCase: GetMembershipStepUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val nfcFileManager: NfcFileManager,
    private val masterSignerMapper: MasterSignerMapper,
    private val saveMembershipStepUseCase: SaveMembershipStepUseCase,
    private val gson: Gson,
    private val updateRemoteSignerUseCase: UpdateRemoteSignerUseCase,
    private val checkRequestAddDesktopKeyStatusUseCase: CheckRequestAddDesktopKeyStatusUseCase,
    private val ncDataStore: NcDataStore,
    private val syncKeyUseCase: SyncKeyUseCase,
    private val syncDraftWalletUseCase: SyncDraftWalletUseCase,
    private val getIndexFromPathUseCase: GetIndexFromPathUseCase,
    private val pushEventManager: PushEventManager,
    private val setKeyVerifiedUseCase: SetKeyVerifiedUseCase,
    private val tapSignerKeyResolver: TapSignerKeyResolver,
) : ViewModel() {
    private val _state = MutableStateFlow(AddKeyListState())
    val state = _state.asStateFlow()
    private val _event = MutableSharedFlow<AddKeyListEvent>()
    val event = _event.asSharedFlow()
    private var loadJob: Job? = null

    private val currentStep =
        savedStateHandle.getStateFlow<MembershipStep?>(KEY_CURRENT_STEP, null)

    private val membershipStepState =
        getMembershipStepUseCase(
            GetMembershipStepUseCase.Param(
                membershipStepManager.localMembershipPlan,
                ""
            )
        ).map { it.getOrElse { emptyList() } }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _keys = MutableStateFlow(listOf<AddKeyData>())
    val key = _keys.asStateFlow()

    private val singleSigners = mutableListOf<SingleSigner>()

    /**
     * xfp of the key being put into the inheritance slot, captured when the add starts. The draft
     * wallet that carries the claim options lands later, so the request has to survive until the
     * data is there. Null when the device has not produced a key yet (a brand-new TAPSIGNER or
     * Coldcard), in which case the first inheritance slot still missing its choice is used.
     */
    private var pendingClaimOptionsXfp: String? = null
    private var promptClaimOptionsOnNextData: Boolean = false

    init {
        viewModelScope.launch {
            currentStep.filterNotNull().collect {
                membershipStepManager.setCurrentStep(it)
            }
        }
        viewModelScope.launch {
            loadSigners()
        }
        viewModelScope.launch {
            // draftSigners is a third source: it lands from refresh() and carries the claim options,
            // so the mapping has to re-run when it arrives, not only when a step or key changes.
            combine(
                membershipStepState,
                key,
                _state.map { it.draftSigners }.distinctUntilChanged(),
            ) { _, keys, _ -> keys }
                .collect { keys ->
                    val coldCardMissingBackupKeys = arrayListOf<AddKeyData>()
                    val news = keys.map { addKeyData ->
                        val info = getStepInfo(addKeyData.type)
                        val extra = runCatching {
                            gson.fromJson(
                                info.extraData,
                                SignerExtra::class.java
                            )
                        }.getOrNull()
                        if (addKeyData.signer == null && info.masterSignerId.isNotEmpty() && extra != null) {
                            loadSigners()
                            val signer =
                                _state.value.signers.find { it.fingerPrint == info.masterSignerId }
                                    ?.copy(
                                        index = getIndexFromPathUseCase(extra.derivationPath)
                                            .getOrDefault(0)
                                    )
                            return@map addKeyData.copy(
                                signer = signer,
                                verifyType = info.verifyType,
                            ).withDraftClaimState(info.masterSignerId, extra)
                        }
                        val newKeyData = addKeyData.copy(verifyType = info.verifyType)
                            .withDraftClaimState(info.masterSignerId, extra)
                        if (newKeyData.owesEncryptedBackup(extra)) {
                            coldCardMissingBackupKeys.add(newKeyData)
                        }
                        return@map newKeyData
                    }
                    _keys.value = news
                    _state.update { it.copy(missingBackupKeys = coldCardMissingBackupKeys) }
                    promptForClaimOptions(news)
                }
        }
        viewModelScope.launch {
            checkRequestAddDesktopKeyStatusUseCase(
                CheckRequestAddDesktopKeyStatusUseCase.Param(
                    membershipStepManager.localMembershipPlan
                )
            )
        }
        viewModelScope.launch {
            pushEventManager.event.collect { event ->
                // A customization change from the AI agent (personal draft, groupId empty)
                // means the draft config may have changed; re-sync to reflect it.
                if (event is PushEvent.DraftWalletCustomizationChanged
                    && event.groupId.isEmpty()
                ) {
                    refresh()
                }
            }
        }
        refresh()
    }

    fun refresh() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isRefresh = true) }
            syncDraftWalletUseCase("").onSuccess { draft ->
                if (draft.walletType == WalletType.MINISCRIPT) {
                    _event.emit(AddKeyListEvent.RequireReopenWallet)
                } else {
                    loadSigners()
                    _state.update {
                        it.copy(
                            walletType = draft.walletType,
                            isCustomized = draft.isCustomized,
                            draftSigners = draft.signers
                                .mapNotNull { signer -> signer.xfp?.let { xfp -> xfp to signer } }
                                .toMap(),
                        )
                    }
                    draft.config.toGroupWalletType()?.let { type ->
                        val steps = type.toSteps(isPersonalWallet = true)
                        if (_keys.value.map { it.type } != steps) {
                            _keys.value = steps.map { step -> AddKeyData(type = step) }
                        }
                    }
                }
            }
            _state.update { it.copy(isRefresh = false) }
        }
    }

    /**
     * Offers the distribution choice for [signer], at most once per key. This runs off a
     * recomputation, not a tap, and the key keeps needing the choice until the server records it —
     * without the guard, backing out of the screen would immediately reopen it.
     */
    private fun promptForClaimOptions(rows: List<AddKeyData>) {
        if (!promptClaimOptionsOnNextData) return
        val wanted = pendingClaimOptionsXfp
        // A group wallet can hold two inheritance keys, so never just take the first one.
        val row = rows.firstOrNull { it.isInheritanceKey && it.signer?.fingerPrint == wanted }
            ?: rows.firstOrNull { it.needsClaimOptions }.takeIf { wanted == null }
        val signer = row?.signer ?: return
        promptClaimOptionsOnNextData = false
        pendingClaimOptionsXfp = null
        _state.update { it.copy(pendingClaimOptionsSigner = signer) }
    }

    /**
     * The keys already on this wallet, so the inheritance picker does not offer one of them for a
     * second slot. Without it the owner can pick a key the draft already holds and the server
     * answers with a duplicate-key error.
     */
    fun existingWalletSigners(): List<SignerModel> = _keys.value.mapNotNull { it.signer }

    /** Called by the add-key paths so the sharing-method choice opens once the key lands. */
    fun onInheritanceKeyAdded(xfp: String? = null) {
        pendingClaimOptionsXfp = xfp
        promptClaimOptionsOnNextData = true
    }

    fun onClaimOptionsPromptHandled() {
        _state.update { it.copy(pendingClaimOptionsSigner = null) }
    }

    /**
     * Overlays what the server knows about this key onto the locally tracked step. [extra] is the
     * local step data, which carries the uploaded backup's file name before the draft catches up.
     */
    private fun AddKeyData.withDraftClaimState(xfp: String, extra: SignerExtra?): AddKeyData {
        val draftSigner = _state.value.draftSigners[xfp]
        return copy(
            claimOptions = draftSigner?.claimOptions.orEmpty(),
            verifications = draftSigner?.verifications.orEmpty(),
            hasEncryptedBackupFile = draftSigner?.userBackUpFileName.isNullOrEmpty().not()
                    || extra?.userKeyFileName.isNullOrEmpty().not(),
            isInheritanceKey = type.isAddInheritanceKey || draftSigner?.isInheritanceKey == true,
        )
    }

    private suspend fun loadSigners() {
        getAllSignersUseCase(false).onSuccess { pair ->
            _state.update {
                singleSigners.apply {
                    clear()
                    addAll(pair.second)
                }
                val signers = pair.first.map { signer ->
                    masterSignerMapper(signer)
                } + pair.second.map { signer -> signer.toModel() }
                it.copy(
                    signers = signers.filter { signer -> signer.derivationPath.isRecommendedMultiSigPath }
                )
            }
        }
    }

    fun onSelectedExistingHardwareSigner(signer: SingleSigner) {
        viewModelScope.launch {
            val actualSigner = if (signer.xpub.isNotEmpty()) signer else singleSigners.find {
                it.masterFingerprint == signer.masterFingerprint
                        && it.derivationPath == signer.derivationPath
            } ?: return@launch
            saveKeyForCurrentStep(actualSigner)
        }
    }

    /**
     * Saves [signer] against the step being filled.
     *
     * Split out from the lookup in [onSelectedExistingHardwareSigner] because a key derived from a
     * TAPSIGNER is already the one to save: it is not in `singleSigners`, which holds remote
     * signers, so putting it through that lookup would drop it without a word.
     */
    private suspend fun saveKeyForCurrentStep(signer: SingleSigner) {
        val step = membershipStepManager.currentStep
            ?: throw IllegalArgumentException("Current step empty")
        // A key added inside the inheritance picker (Coldcard via NFC/QR/file, air-gap) was put on
        // the draft and saved against this step by that flow already. Asking the server to add it
        // again is what it answers with "Duplicate key xfp", and re-saving the step would also
        // overwrite the flow's own verification state, so there is nothing left to do here.
        if (membershipStepManager.isKeySavedForStep(step, signer.masterFingerprint)) return
        syncKeyUseCase(
            SyncKeyUseCase.Param(
                step = step,
                signer = signer,
                walletType = _state.value.walletType ?: WalletType.MULTI_SIG
            )
        ).onFailure {
            _event.emit(AddKeyListEvent.ShowError(it.message.orUnknownError()))
            return
        }
        saveMembershipStepUseCase(
            MembershipStepInfo(
                step = step,
                masterSignerId = signer.masterFingerprint,
                plan = membershipStepManager.localMembershipPlan,
                verifyType = VerifyType.APP_VERIFIED,
                extraData = gson.toJson(
                    SignerExtra(
                        derivationPath = signer.derivationPath,
                        isAddNew = false,
                        signerType = signer.type,
                        userKeyFileName = ""
                    )
                ),
                groupId = ""
            )
        )
        if (step.isAddInheritanceKey) {
            onInheritanceKeyAdded(signer.masterFingerprint)
        }
    }

    fun onUpdateSignerTag(signer: SignerModel, tag: SignerTag) {
        viewModelScope.launch {
            singleSigners.find { it.masterFingerprint == signer.fingerPrint && it.derivationPath == signer.derivationPath }
                ?.let { singleSigner ->
                    updateRemoteSignerUseCase(singleSigner.copy(tags = listOf(tag)))
                        .onSuccess {
                            loadSigners()
                            onSelectedExistingHardwareSigner(singleSigner.copy(tags = listOf(tag)))
                        }.onFailure {
                            _event.emit(AddKeyListEvent.ShowError(it.message.orUnknownError()))
                        }
                }
        }
    }

    fun onAddKeyClicked(data: AddKeyData) {
        viewModelScope.launch {
            savedStateHandle[KEY_CURRENT_STEP] = data.type
            _event.emit(AddKeyListEvent.OnAddKey(data))
        }
    }

    private fun isSignerExist(masterSignerId: String) =
        membershipStepManager.isKeyExisted(masterSignerId)

    /**
     * [claimOption] names the artifact being verified. It defaults to what the row knows, and is
     * passed explicitly right after the owner picks a sharing method, when the row has not caught
     * up with the server yet.
     */
    fun onVerifyClicked(
        data: AddKeyData,
        claimOption: ClaimOption? = data.encryptedBackupClaimOption(),
    ) {
        data.signer?.let { signer ->
            savedStateHandle[KEY_CURRENT_STEP] = data.type
            viewModelScope.launch {
                val stepInfo = getStepInfo(data.type)
                _event.emit(
                    AddKeyListEvent.OnVerifySigner(
                        signer = signer,
                        filePath = nfcFileManager.buildFilePath(stepInfo.keyIdInServer.ifEmpty { data.signer.fingerPrint }),
                        backUpFileName = getBackUpFileName(stepInfo.extraData),
                        claimOption = claimOption,
                    )
                )
            }
        }
    }

    /**
     * Records that the seed-phrase backup of the inheritance key was proven: the owner restored it
     * onto a device and the device reported the fingerprint of the inheritance key back.
     *
     * Only the keys that pair in-app (Ledger, BitBox) come back here — Coldcard and air-gap mark
     * themselves verified inside their own add-key screens and never return a fingerprint.
     */
    fun onSeedPhraseBackupVerified(masterSignerId: String) {
        viewModelScope.launch {
            setKeyVerifiedUseCase(
                SetKeyVerifiedUseCase.Param(
                    groupId = "",
                    masterSignerId = masterSignerId,
                    verifyType = VerifyType.APP_VERIFIED,
                    verificationMethod = ClaimOption.SEED_PHRASE,
                )
            ).onSuccess {
                refresh()
                _event.emit(
                    AddKeyListEvent.OnSeedPhraseBackupVerified(
                        signer = _keys.value.firstOrNull {
                            it.signer?.fingerPrint.equals(masterSignerId, ignoreCase = true)
                        }?.signer
                    )
                )
            }.onFailure {
                _event.emit(AddKeyListEvent.ShowError(it.message.orUnknownError()))
            }
        }
    }

    /**
     * A TAPSIGNER picked for the inheritance slot. It is a master signer, so the key for the
     * wallet has to be derived from it, and reading that may need another tap on the card.
     */
    fun addExistingTapSignerKey(signerModel: SignerModel) {
        viewModelScope.launch {
            handleTapSignerOutcome(tapSignerKeyResolver.resolve(signerModel.fingerPrint))
        }
    }

    /** The card was tapped for the xpub the resolver asked for. */
    fun onTapSignerCardTapped(isoDep: android.nfc.tech.IsoDep?, cvc: String) {
        isoDep ?: return
        viewModelScope.launch {
            handleTapSignerOutcome(tapSignerKeyResolver.resolveAfterCardTap(isoDep, cvc))
        }
    }

    fun onTapSignerCardTapHandled() {
        _state.update { it.copy(requestTapSignerCardTap = false) }
    }

    private suspend fun handleTapSignerOutcome(outcome: TapSignerKeyResolver.Outcome) {
        when (outcome) {
            is TapSignerKeyResolver.Outcome.Resolved -> saveKeyForCurrentStep(outcome.signer)
            TapSignerKeyResolver.Outcome.NeedsCardTap ->
                _state.update { it.copy(requestTapSignerCardTap = true) }

            is TapSignerKeyResolver.Outcome.Failed ->
                _event.emit(AddKeyListEvent.ShowError(outcome.message))
        }
    }

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(AddKeyListEvent.OnAddAllKey)
        }
    }

    private fun getStepInfo(step: MembershipStep) =
        membershipStepState.value.find { it.step == step } ?: run {
            MembershipStepInfo(
                step = step,
                plan = membershipStepManager.localMembershipPlan,
                groupId = ""
            )
        }

    fun getTapSigners() =
        _state.value.signers.filter { it.type == SignerType.NFC && isSignerExist(it.fingerPrint).not() }

    fun getColdcard() = _state.value.signers.filter {
        isSignerExist(it.fingerPrint).not()
                && ((it.type == SignerType.COLDCARD_NFC && it.derivationPath.isRecommendedMultiSigPath)
                || (it.type == SignerType.AIRGAP && (it.tags.isEmpty() || it.tags.contains(SignerTag.COLDCARD))))
    }

    fun getHardwareSigners(tag: SignerTag) =
        _state.value.signers.filter {
            isSignerExist(it.fingerPrint).not() && it.type == SignerType.HARDWARE && it.tags.contains(
                tag
            )
        }

    fun getAirgap(tag: SignerTag?): List<SignerModel> {
        return if (tag == null) {
            _state.value.signers.filter {
                it.type == SignerType.AIRGAP
                        && isSignerExist(it.fingerPrint).not()
            }
        } else {
            _state.value.signers.filter {
                it.type == SignerType.AIRGAP
                        && isSignerExist(it.fingerPrint).not()
                        && (it.tags.contains(tag) || it.tags.isEmpty())
            }
        }
    }

    fun getPortal(): List<SignerModel> =
        _state.value.signers.filter { it.type == SignerType.PORTAL_NFC && isSignerExist(it.fingerPrint).not() }

    private fun getBackUpFileName(extra: String): String {
        return runCatching {
            gson.fromJson(extra, SignerExtra::class.java).userKeyFileName
        }.getOrDefault("")
    }

    companion object {
        private const val KEY_CURRENT_STEP = "current_step"
    }
}

sealed class AddKeyListEvent {
    data class OnAddKey(val data: AddKeyData) : AddKeyListEvent()
    data class OnVerifySigner(
        val signer: SignerModel,
        val filePath: String,
        val backUpFileName: String,
        /**
         * Which sharing method this backup resolves, for an off-chain inheritance key. These
         * screens only ever handle the encrypted backup, and a "do both" key needs the server
         * told which of its two artifacts was verified.
         */
        val claimOption: ClaimOption? = null,
    ) : AddKeyListEvent()

    data object OnAddAllKey : AddKeyListEvent()

    /** The restored device matched, so the seed-phrase backup of [signer] is proven. */
    data class OnSeedPhraseBackupVerified(val signer: SignerModel?) : AddKeyListEvent()

    data object SelectAirgapType : AddKeyListEvent()
    data object RequireReopenWallet : AddKeyListEvent()
    data class ShowError(val message: String) : AddKeyListEvent()
}

data class AddKeyListState(
    /** The resolver needs the TAPSIGNER tapped to read the xpub it could not find cached. */
    val requestTapSignerCardTap: Boolean = false,
    val isLoading: Boolean = false,
    val isRefresh: Boolean = false,
    val signers: List<SignerModel> = emptyList(),
    val walletType: WalletType? = null,
    val isCustomized: Boolean = false,
    val missingBackupKeys: List<AddKeyData> = emptyList(),
    /**
     * Signers as the server sees them on the draft wallet, keyed by xfp. The local step only knows
     * that a key was added; the claim options and per-method verifications of an inheritance key
     * live here.
     */
    val draftSigners: Map<String, SignerServer> = emptyMap(),
    /** Inheritance key waiting for the owner to pick how it reaches their Beneficiary. */
    val pendingClaimOptionsSigner: SignerModel? = null
)