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

package com.nunchuk.android.main.membership.byzantine.addKey

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
import com.nunchuk.android.main.membership.key.TapSignerKeyResolver
import com.nunchuk.android.main.membership.model.AddKeyData
import com.nunchuk.android.main.membership.model.encryptedBackupClaimOption
import com.nunchuk.android.main.membership.model.owesEncryptedBackup
import com.nunchuk.android.main.membership.model.toGroupWalletType
import com.nunchuk.android.main.membership.model.toSteps
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.model.SignerExtra
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.inheritance.ClaimOption
import com.nunchuk.android.model.signer.SignerServer
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.model.isAddInheritanceKey
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.GetIndexFromPathUseCase
import com.nunchuk.android.usecase.UpdateRemoteSignerUseCase
import com.nunchuk.android.usecase.membership.GetMembershipStepUseCase
import com.nunchuk.android.usecase.membership.SaveMembershipStepUseCase
import com.nunchuk.android.usecase.membership.SetKeyVerifiedUseCase
import com.nunchuk.android.usecase.membership.SyncDraftWalletUseCase
import com.nunchuk.android.usecase.membership.SyncKeyUseCase
import com.nunchuk.android.usecase.signer.GetAllSignersUseCase
import com.nunchuk.android.usecase.wallet.GetWallets2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddByzantineKeyListViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    getMembershipStepUseCase: GetMembershipStepUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val nfcFileManager: NfcFileManager,
    private val masterSignerMapper: MasterSignerMapper,
    private val saveMembershipStepUseCase: SaveMembershipStepUseCase,
    private val gson: Gson,
    private val updateRemoteSignerUseCase: UpdateRemoteSignerUseCase,
    private val syncKeyUseCase: SyncKeyUseCase,
    private val syncDraftWalletUseCase: SyncDraftWalletUseCase,
    private val pushEventManager: PushEventManager,
    private val getAllSignersUseCase: GetAllSignersUseCase,
    private val getIndexFromPathUseCase: GetIndexFromPathUseCase,
    private val ncDataStore: NcDataStore,
    private val getWallets2UseCase: GetWallets2UseCase,
    private val setKeyVerifiedUseCase: SetKeyVerifiedUseCase,
    private val tapSignerKeyResolver: TapSignerKeyResolver,
) : ViewModel() {
    private val _state = MutableStateFlow(AddKeyListState())
    val state = _state.asStateFlow()
    private val _event = MutableSharedFlow<AddKeyListEvent>()
    val event = _event.asSharedFlow()
    private val args: AddByzantineKeyListFragmentArgs =
        AddByzantineKeyListFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val currentStep =
        savedStateHandle.getStateFlow<MembershipStep?>(KEY_CURRENT_STEP, null)

    private val membershipStepState = getMembershipStepUseCase(
        GetMembershipStepUseCase.Param(
            MembershipPlan.NONE,
            args.groupId
        )
    ).map { it.getOrElse { emptyList() } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * xfp of the key being put into the inheritance slot, captured when the add starts. The draft
     * wallet that carries the claim options lands later, so the request has to survive until the
     * data is there. Null when the device has not produced a key yet (a brand-new TAPSIGNER or
     * Coldcard), in which case the first inheritance slot still missing its choice is used.
     */
    private var pendingClaimOptionsXfp: String? = null
    private var promptClaimOptionsOnNextData: Boolean = false

    private val _keys = MutableStateFlow(listOf<AddKeyData>())
    val key = _keys.asStateFlow()

    private val singleSigners = mutableListOf<SingleSigner>()
    private val unBackedUpSignerXfpSet = mutableSetOf<String>()

    init {
        if (args.isAddOnly) {
            viewModelScope.launch {
                pushEventManager.event.filterIsInstance<PushEvent.KeyAddedToGroup>().collect {
                    _state.update { it.copy(shouldShowKeyAdded = true) }
                }
            }
        }
        viewModelScope.launch {
            currentStep.filterNotNull().collect {
                membershipStepManager.setCurrentStep(it)
            }
        }
        refresh()
        viewModelScope.launch {
            membershipStepState.combine(key) { _, key -> key }.collect {
                loadSigners()
                updateKeyData()
            }
        }
        getUnBackedUpWallet()
    }

    private suspend fun loadSigners() {
        getAllSignersUseCase(false).onSuccess { pair ->
            val singleSigner = pair.second.distinctBy { it.masterFingerprint }
            singleSigners.apply {
                clear()
                addAll(singleSigner)
            }
            val signers = pair.first.map { signer ->
                masterSignerMapper(signer)
            } + singleSigner.map { signer -> signer.toModel() }
            _state.update { it.copy(signers = signers.filter { signer -> signer.derivationPath.isRecommendedMultiSigPath }) }
            updateKeyData()
        }
    }

    private suspend fun updateKeyData() {
        if (key.value.isEmpty()) return
        val signers = _state.value.signers
        val coldCardMissingBackupKeys = mutableListOf<AddKeyData>()
        val news = key.value.map { addKeyData ->
            val info = getStepInfo(addKeyData.type)
            var signer =
                if (info.masterSignerId.isNotEmpty()) signers.find { it.fingerPrint == info.masterSignerId } else null
            var extra: SignerExtra? = null
            if (signer != null) {
                runCatching {
                    extra = gson.fromJson(info.extraData, SignerExtra::class.java)
                    signer = signer?.copy(
                        index = getIndexFromPathUseCase(extra!!.derivationPath).getOrDefault(0)
                    )
                }
            }
            val draftSigner = _state.value.draftSigners[info.masterSignerId]
            val newKeyData = addKeyData.copy(
                signer = signer,
                verifyType = info.verifyType,
                claimOptions = draftSigner?.claimOptions.orEmpty(),
                verifications = draftSigner?.verifications.orEmpty(),
                isInheritanceKey = addKeyData.type.isAddInheritanceKey || draftSigner?.isInheritanceKey == true,
            )
            if (newKeyData.owesEncryptedBackup(extra)) {
                coldCardMissingBackupKeys.add(newKeyData)
            }
            return@map newKeyData
        }
        _state.update { it.copy(missingBackupKeys = coldCardMissingBackupKeys) }
        _keys.value = news
        promptForClaimOptions(news)
    }

    /**
     * Offers the distribution choice for [signer], at most once per key. This runs off
     * [updateKeyData], a recomputation rather than a tap, and the key keeps needing the choice
     * until the server records it — without the guard, backing out would immediately reopen it.
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

    /** Called by the add-key paths so the sharing-method choice opens once the key lands. */
    fun onInheritanceKeyAdded(xfp: String? = null) {
        pendingClaimOptionsXfp = xfp
        promptClaimOptionsOnNextData = true
    }

    fun onClaimOptionsPromptHandled() {
        _state.update { it.copy(pendingClaimOptionsSigner = null) }
    }

    fun onUpdateSignerTag(signer: SignerModel, tag: SignerTag) {
        viewModelScope.launch {
            singleSigners.find { it.masterFingerprint == signer.fingerPrint && it.derivationPath == signer.derivationPath }
                ?.let { singleSigner ->
                    updateRemoteSignerUseCase(singleSigner.copy(tags = listOf(tag)))
                        .onSuccess {
                            loadSigners()
                            _event.emit(AddKeyListEvent.UpdateSignerTag(signer.copy(tags = listOf(tag))))
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
                        filePath = nfcFileManager.buildFilePath(stepInfo.keyIdInServer.ifEmpty { signer.fingerPrint }),
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
                    groupId = args.groupId,
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
            is TapSignerKeyResolver.Outcome.Resolved -> handleSignerNewIndex(outcome.signer)
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
            MembershipStepInfo(step = step, groupId = args.groupId)
        }

    fun getTapSigners() =
        _state.value.signers.filter { it.type == SignerType.NFC && isSignerExist(it.fingerPrint).not() }

    fun getColdcard() = _state.value.signers.filter {
        (it.type == SignerType.COLDCARD_NFC
                && it.derivationPath.isRecommendedMultiSigPath
                && isSignerExist(it.fingerPrint).not()) || (it.type == SignerType.AIRGAP && it.tags.isEmpty())
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

    fun getHardwareSigners(tag: SignerTag) =
        _state.value.signers.filter { it.type == SignerType.HARDWARE && it.tags.contains(tag) }

    fun getSoftwareSigners() =
        _state.value.signers.filter { (it.type == SignerType.SOFTWARE || it.type == SignerType.FOREIGN_SOFTWARE) && isSignerExist(it.fingerPrint).not() }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            syncDraftWalletUseCase(args.groupId).onSuccess { draft ->
                _state.update {
                    it.copy(
                        groupWalletType = draft.config.toGroupWalletType(),
                        walletType = draft.walletType,
                        draftSigners = draft.signers
                            .mapNotNull { signer -> signer.xfp?.let { xfp -> xfp to signer } }
                            .toMap(),
                    )
                }
                loadSigners()
                draft.config.toGroupWalletType()?.let { type ->
                    if (_keys.value.isEmpty()) {
                        _keys.value = type.toSteps().map { step -> AddKeyData(type = step) }
                    }
                }
            }
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun markHandledShowKeyAdded() {
        _state.update { it.copy(shouldShowKeyAdded = false) }
    }

    fun handleSignerNewIndex(signer: SingleSigner) {
        viewModelScope.launch {
            syncKeyUseCase(
                SyncKeyUseCase.Param(
                    groupId = args.groupId,
                    step = membershipStepManager.currentStep
                        ?: throw IllegalArgumentException("Current step empty"),
                    signer = signer,
                    walletType = _state.value.walletType ?: WalletType.MULTI_SIG
                )
            ).onFailure {
                _event.emit(AddKeyListEvent.ShowError(it.message.orUnknownError()))
                return@launch
            }
            saveMembershipStepUseCase(
                MembershipStepInfo(
                    step = membershipStepManager.currentStep
                        ?: throw IllegalArgumentException("Current step empty"),
                    masterSignerId = signer.masterFingerprint,
                    plan = MembershipPlan.NONE,
                    verifyType = VerifyType.APP_VERIFIED,
                    extraData = gson.toJson(
                        SignerExtra(
                            derivationPath = signer.derivationPath,
                            isAddNew = false,
                            signerType = signer.type,
                            userKeyFileName = ""
                        )
                    ),
                    groupId = args.groupId
                )
            )
            // Only an inheritance key needs the draft re-synced here: the claim options that decide
            // what comes next live there, and the local step alone cannot tell us.
            if (membershipStepManager.currentStep?.isAddInheritanceKey == true) {
                onInheritanceKeyAdded(signer.masterFingerprint)
                refresh()
            }
        }
    }

    fun getGroupWalletType() = _state.value.groupWalletType
    fun getCountWalletSoftwareSignersInDevice() = key.value.count { it.signer != null && it.signer.type == SignerType.SOFTWARE && it.signer.isVisible }
    fun getPortalSigners() = _state.value.signers.filter { it.type == SignerType.PORTAL_NFC && isSignerExist(it.fingerPrint).not() }

    private fun getUnBackedUpWallet() {
        viewModelScope.launch {
            getWallets2UseCase(Unit)
                .onSuccess { wallets ->
                    wallets.filter { it.needBackup }.forEach {
                        unBackedUpSignerXfpSet.add(it.signers.first().masterFingerprint)
                    }
                }
        }
    }

    fun isUnBackedUpSigner(signer: SignerModel) = unBackedUpSignerXfpSet.contains(signer.fingerPrint)

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
    data class ShowError(val message: String) : AddKeyListEvent()
    data class UpdateSignerTag(val signer: SignerModel) : AddKeyListEvent()
}

data class AddKeyListState(
    /** The resolver needs the TAPSIGNER tapped to read the xpub it could not find cached. */
    val requestTapSignerCardTap: Boolean = false,
    val isRefreshing: Boolean = false,
    val signers: List<SignerModel> = emptyList(),
    val similarGroups: Map<String, String> = emptyMap(),
    val shouldShowKeyAdded: Boolean = false,
    val groupWalletType: GroupWalletType? = null,
    val walletType: WalletType? = null,
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