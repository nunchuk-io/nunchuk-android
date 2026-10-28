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

package com.nunchuk.android.main.membership.onchaintimelock.addkey

import androidx.annotation.StringRes
import com.nunchuk.android.model.inheritance.bip32AccountIndex
import com.nunchuk.android.model.inheritance.DraftKeyAccountMismatchException
import com.nunchuk.android.model.inheritance.DraftKeySlotMissingException
import com.nunchuk.android.model.inheritance.RecoveredKeyRequiredException
import com.nunchuk.android.core.signer.OnChainInheritanceGuide
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.nunchuk.android.core.domain.settings.GetChainSettingFlowUseCase
import com.nunchuk.android.core.domain.signer.GetSignerFromTapsignerMasterSignerByPathUseCase
import com.nunchuk.android.core.domain.utils.NfcFileManager
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.mapper.SingleSignerMapper
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.exception.NCNativeException
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.model.AddKeyOnChainData
import com.nunchuk.android.main.membership.model.toAddKeyOnChainDataList
import com.nunchuk.android.main.membership.model.toGroupWalletType
import com.nunchuk.android.main.membership.model.toSteps
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.SignerExtra
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.byzantine.DraftWallet
import com.nunchuk.android.model.signer.SignerServer
import com.nunchuk.android.model.inheritance.RecoveredKey
import com.nunchuk.android.model.inheritance.DraftKeySlotHolder
import com.nunchuk.android.model.inheritance.DraftInheritanceCard
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.model.inheritance.FallbackInheritanceCondition
import com.nunchuk.android.model.inheritance.InheritanceSection
import com.nunchuk.android.model.inheritance.InheritanceValidationError
import com.nunchuk.android.model.inheritance.OnChainInheritancePolicy
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.GetIndexFromPathUseCase
import com.nunchuk.android.usecase.GetRemoteSignerUseCase
import com.nunchuk.android.usecase.UpdateRemoteSignerUseCase
import com.nunchuk.android.usecase.membership.CheckRequestAddDesktopKeyStatusUseCase
import com.nunchuk.android.usecase.membership.GetMembershipStepUseCase
import com.nunchuk.android.usecase.membership.SaveMembershipStepUseCase
import com.nunchuk.android.usecase.membership.SetKeyVerifiedUseCase
import com.nunchuk.android.usecase.membership.SyncDraftWalletUseCase
import com.nunchuk.android.usecase.membership.SyncKeyUseCase
import com.nunchuk.android.usecase.signer.GetAllSignersUseCase
import com.nunchuk.android.usecase.signer.GetSignerFromMasterSignerByIndexUseCase
import com.nunchuk.android.usecase.signer.GetUnusedSignerFromMasterSignerV2UseCase
import com.nunchuk.android.usecase.wallet.AddDraftFallbackInheritanceUseCase
import com.nunchuk.android.usecase.wallet.RemoveDraftFallbackInheritanceUseCase
import com.nunchuk.android.usecase.wallet.ResetDraftPrimaryInheritanceUseCase
import com.nunchuk.android.usecase.wallet.UpdateDraftFallbackInheritanceUseCase
import com.nunchuk.android.usecase.wallet.UpdateDraftPrimaryInheritanceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class OnChainTimelockAddKeyListViewModel @Inject constructor(
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
    private val syncKeyUseCase: SyncKeyUseCase,
    private val syncDraftWalletUseCase: SyncDraftWalletUseCase,
    private val getIndexFromPathUseCase: GetIndexFromPathUseCase,
    private val getSignerFromMasterSignerByIndexUseCase: GetSignerFromMasterSignerByIndexUseCase,
    private val getRemoteSignerUseCase: GetRemoteSignerUseCase,
    private val getUnusedSignerFromMasterSignerV2UseCase: GetUnusedSignerFromMasterSignerV2UseCase,
    private val getSignerFromTapsignerMasterSignerByPathUseCase: GetSignerFromTapsignerMasterSignerByPathUseCase,
    private val singleSignerMapper: SingleSignerMapper,
    private val pushEventManager: com.nunchuk.android.core.push.PushEventManager,
    private val getChainSettingFlowUseCase: GetChainSettingFlowUseCase,
    private val setKeyVerifiedUseCase: SetKeyVerifiedUseCase,
    private val updateDraftPrimaryInheritanceUseCase: UpdateDraftPrimaryInheritanceUseCase,
    private val resetDraftPrimaryInheritanceUseCase: ResetDraftPrimaryInheritanceUseCase,
    private val addDraftFallbackInheritanceUseCase: AddDraftFallbackInheritanceUseCase,
    private val updateDraftFallbackInheritanceUseCase: UpdateDraftFallbackInheritanceUseCase,
    private val removeDraftFallbackInheritanceUseCase: RemoveDraftFallbackInheritanceUseCase,
    private val draftKeySlotHolder: DraftKeySlotHolder,
) : ViewModel() {
    private val _state = MutableStateFlow(AddKeyListState())
    val state = _state.asStateFlow()
    private val _event = MutableSharedFlow<AddKeyListEvent>()
    val event = _event.asSharedFlow()
    private var loadJob: Job? = null
    private var refreshAgain = false

    // Get args from SavedStateHandle (for navigation arguments)
    val groupId: String = savedStateHandle.get<String>("group_id") ?: ""
    val isAddOnly: Boolean = savedStateHandle.get<Boolean>("is_add_only") ?: false
    private val configArg: com.nunchuk.android.model.WalletConfig? = savedStateHandle.get<com.nunchuk.android.model.WalletConfig>("config")
    val isGroupWallet: Boolean get() = groupId.isNotEmpty()

    private val currentStep =
        savedStateHandle.getStateFlow<MembershipStep?>(KEY_CURRENT_STEP, null)

    private val membershipStepState =
        getMembershipStepUseCase(
            GetMembershipStepUseCase.Param(
                plan = if (isGroupWallet) MembershipPlan.NONE else membershipStepManager.localMembershipPlan,
                groupId = groupId
            )
        ).map { it.getOrElse { emptyList() } }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _keys = MutableStateFlow(listOf<AddKeyOnChainData>())
    val key = _keys.asStateFlow()

    private val singleSigners = mutableListOf<SingleSigner>()
    private val masterSigners = mutableListOf<MasterSigner>()
    private var isTestNet: Boolean = false

    /**
     * The account the configurable-draft slot being filled needs; null outside such an add. Saved, as
     * the device flow it waits on can outlive this process.
     */
    private var activeDraftAccount: Int?
        get() = savedStateHandle.get<Int>(KEY_ACTIVE_DRAFT_ACCOUNT)
        set(value) {
            if (value == null) savedStateHandle.remove<Int>(KEY_ACTIVE_DRAFT_ACCOUNT)
            else savedStateHandle[KEY_ACTIVE_DRAFT_ACCOUNT] = value
        }

    // Context for TapSigner caching
    private var pendingTapSignerData: AddKeyOnChainData? = null
    private var pendingTapSignerWalletId: String? = null
    private var pendingTapSignerSignerModel: SignerModel? = null

    init {
        if (savedStateHandle.get<Boolean>(KEY_INHERITANCE_GUIDE_SEEN) == true) {
            _state.update { it.copy(isInheritanceGuideSeenInSession = true) }
        }
        savedStateHandle.get<IntArray>(KEY_FALLBACK_GUIDES_SEEN)?.let { seen ->
            _state.update { it.copy(fallbackGuidesSeenInSession = seen.toSet()) }
        }
        configArg?.let { config ->
            initializeKeysFromConfig(config)
        }
        
        // Handle key added event for group wallet (isAddOnly mode)
        if (isAddOnly) {
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
        viewModelScope.launch {
            getChainSettingFlowUseCase(Unit)
                .map { it.getOrDefault(Chain.MAIN) }
                .collect {
                    isTestNet = it == Chain.TESTNET
                }
        }
        viewModelScope.launch {
            loadSigners()
        }
        viewModelScope.launch {
            membershipStepState.combine(key) { _, keys -> keys }
                .collect { keys ->
                    val nfcMissingBackupKeys = arrayListOf<AddKeyOnChainData>()

                    val updatedKeys = keys.map { addKeyData ->
                        var updatedCard = addKeyData

                        // Process each step in the card
                        addKeyData.steps.forEach { step ->
                            val info = getStepInfo(step)
                            val extra = runCatching {
                                gson.fromJson(
                                    info.extraData,
                                    SignerExtra::class.java
                                )
                            }.getOrNull()

                            // If step has a master signer ID and extra data, try to find and add the signer
                            if (info.masterSignerId.isNotEmpty() && extra != null) {
                                loadSigners()
                                var signer =
                                    _state.value.signers.find { it.fingerPrint == info.masterSignerId }
                                signer = signer?.copy(
                                    index = getIndexFromPathUseCase(extra.derivationPath)
                                        .getOrDefault(0),
                                    derivationPath = extra.derivationPath.ifEmpty { signer.derivationPath }
                                )

                                if (signer != null) {
                                    updatedCard =
                                        updatedCard.updateStep(step, signer, info.verifyType)
                                }
                            } else if (step == MembershipStep.ADD_SEVER_KEY || step == MembershipStep.TIMELOCK) {
                                updatedCard = updatedCard.updateStep(
                                    step,
                                    null,
                                    info.verifyType,
                                    timelock = if (step == MembershipStep.TIMELOCK) info.parseTimelockExtra() else null
                                )
                            }

                            if (addKeyData.signers?.firstOrNull()?.type == SignerType.NFC) {
                                if (extra != null && extra.userKeyFileName.isEmpty()) {
                                    nfcMissingBackupKeys.add(updatedCard)
                                }
                            }
                        }

                        updatedCard
                    }
                    _keys.value = updatedKeys
                    _state.update { it.copy(missingBackupKeys = nfcMissingBackupKeys) }
                }
        }
        // Only check desktop key status for personal wallet
        if (!isGroupWallet) {
            viewModelScope.launch {
                checkRequestAddDesktopKeyStatusUseCase(
                    CheckRequestAddDesktopKeyStatusUseCase.Param(
                        membershipStepManager.localMembershipPlan
                    )
                )
            }
        }
        viewModelScope.launch {
            pushEventManager.event.collect { event ->
                when (event) {
                    is PushEvent.DraftWalletTimelockSet -> {
                        if (!isGroupWallet || event.groupId == groupId) {
                            refresh()
                        }
                    }

                    is PushEvent.DraftWalletCustomizationChanged -> {
                        if (!isGroupWallet || event.groupId == groupId) {
                            refresh()
                        }
                    }

                    is PushEvent.DraftWalletUpdated -> {
                        if (!isGroupWallet || event.groupId == groupId) {
                            refresh()
                        }
                    }

                    else -> {}
                }
            }
        }
        refresh()
    }

    fun refresh() {
        // A load already running may have fetched the draft before the change that asked for this
        // one (e.g. a key added while returning from the guide), so run once more after it.
        if (loadJob?.isActive == true) {
            refreshAgain = true
            return
        }
        loadJob = viewModelScope.launch {
            do {
                refreshAgain = false
                loadDraft()
            } while (refreshAgain)
        }
    }

    private suspend fun loadDraft() {
        _state.update { it.copy(isRefresh = true) }
        syncDraftWalletUseCase(groupId).onSuccess { draft ->
            if (draft.walletType == WalletType.MULTI_SIG) {
                _event.emit(AddKeyListEvent.RequireReopenWallet)
            } else {
                _state.update { it.copy(isCustomized = draft.isCustomized) }
                applyInheritancePolicy(draft)
                initializeKeysFromConfig(draft.config)
                loadSigners()
                rebuildDraftCards()
            }
        }
        _state.update { it.copy(isRefresh = false) }
    }

    private fun initializeKeysFromConfig(config: com.nunchuk.android.model.WalletConfig) {
        _state.update {
            it.copy(
                walletType = WalletType.MINISCRIPT,
                groupWalletType = config.toGroupWalletType()
            )
        }
        config.toGroupWalletType()?.let { type ->
            if (_keys.value.isEmpty()) {
                val steps = type.toSteps(
                    isPersonalWallet = !isGroupWallet,
                    walletType = WalletType.MINISCRIPT
                )
                _keys.value = steps.toAddKeyOnChainDataList()
            }
        }
    }

    private suspend fun loadSigners() {
        getAllSignersUseCase(false).onSuccess { pair ->
            _state.update {
                val singleSigner = pair.second.distinctBy { it.masterFingerprint }
                singleSigners.apply {
                    clear()
                    addAll(pair.second)
                }
                masterSigners.apply {
                    clear()
                    addAll(pair.first)
                }
                val signers = pair.first.map { signer ->
                    masterSignerMapper(signer)
                } + singleSigner.map { signer -> signer.toModel() }
                it.copy(
                    signers = signers
                )
            }
        }
    }

    fun onSelectedExistingHardwareSigner(signer: SingleSigner, keyData: AddKeyOnChainData? = null) {
        viewModelScope.launch {
            val actualSigner = if (signer.xpub.isNotEmpty()) signer else singleSigners.find {
                it.masterFingerprint == signer.masterFingerprint
                        && it.derivationPath == signer.derivationPath
            } ?: return@launch

            val currentStep = membershipStepManager.currentStep
                ?: throw IllegalArgumentException("Current step empty")

            syncKeyUseCase(
                SyncKeyUseCase.Param(
                    step = currentStep,
                    signer = actualSigner,
                    walletType = WalletType.MINISCRIPT,
                    groupId = groupId
                )
            ).onFailure {
                _event.emit(it.toAddKeyErrorEvent())
                return@launch
            }
            val verifyType = getVerificationTypeForSigner(signer.toModel(), keyData)
            saveMembershipStepUseCase(
                MembershipStepInfo(
                    step = currentStep,
                    masterSignerId = signer.masterFingerprint,
                    plan = membershipStepManager.localMembershipPlan,
                    verifyType = verifyType,
                    extraData = gson.toJson(
                        SignerExtra(
                            derivationPath = signer.derivationPath,
                            isAddNew = false,
                            signerType = signer.type,
                            userKeyFileName = ""
                        )
                    ),
                    groupId = groupId
                )
            )

            // Update the card with the new signer for the current step
            updateCardForStep(currentStep, signer.toModel(), verifyType)
            onDraftKeySynced()
        }
    }

    private fun updateCardForStep(
        step: MembershipStep,
        signer: SignerModel,
        verifyType: VerifyType
    ) {
        val currentKeys = _keys.value
        val cardIndex = currentKeys.indexOfFirst { it.steps.contains(step) }

        if (cardIndex != -1) {
            val card = currentKeys[cardIndex]
            val updatedCard = card.updateStep(step, signer, verifyType)
            _keys.value = currentKeys.toMutableList().apply {
                set(cardIndex, updatedCard)
            }
        }
    }

    fun onUpdateSignerTag(signer: SignerModel, tag: SignerTag) {
        viewModelScope.launch {
            singleSigners.find { it.masterFingerprint == signer.fingerPrint && it.derivationPath == signer.derivationPath }
                ?.let { singleSigner ->
                    updateRemoteSignerUseCase(singleSigner.copy(tags = listOf(tag)))
                        .onSuccess {
                            loadSigners()
                            if (isGroupWallet) {
                                _event.emit(
                                    AddKeyListEvent.UpdateSignerTag(
                                        signer.copy(tags = listOf(tag))
                                    )
                                )
                            } else {
                                onSelectedExistingHardwareSigner(singleSigner.copy(tags = listOf(tag)))
                            }
                        }.onFailure {
                            _event.emit(AddKeyListEvent.ShowError(it.message.orUnknownError()))
                        }
                }
        }
    }

    /**
     * The device restored from the seed-phrase backup reported [masterSignerId]; if that is the key
     * on the card, the backup is good. Same contract as the Coldcard and air-gap verify steps.
     */
    fun setKeyVerified(masterSignerId: String, recoveredKey: RecoveredKey? = null) {
        viewModelScope.launch {
            setKeyVerifiedUseCase(
                SetKeyVerifiedUseCase.Param(
                    groupId = groupId,
                    masterSignerId = masterSignerId,
                    verifyType = VerifyType.APP_VERIFIED,
                    recoveredKey = recoveredKey,
                )
            ).onSuccess {
                refresh()
                _event.emit(AddKeyListEvent.OnKeyVerified)
            }.onFailure {
                _event.emit(it.toAddKeyErrorEvent())
            }
        }
    }

    /** A wrong account for a configurable-draft slot names the account to add. */
    private fun Throwable.toAddKeyErrorEvent(): AddKeyListEvent {
        if (this is DraftKeySlotMissingException) return AddKeyListEvent.ShowErrorRes(R.string.nc_draft_key_slot_missing)
        if (this is RecoveredKeyRequiredException) return AddKeyListEvent.ShowErrorRes(R.string.nc_draft_key_account_not_read)
        if (this !is DraftKeyAccountMismatchException) return AddKeyListEvent.ShowError(message.orUnknownError())
        val account = expectedAccount
            ?: return AddKeyListEvent.ShowErrorRes(R.string.nc_draft_key_account_unavailable)
        return AddKeyListEvent.ShowErrorRes(R.string.nc_draft_key_wrong_account, listOf(account))
    }

    fun markHandledShowKeyAdded() {
        _state.update { it.copy(shouldShowKeyAdded = false) }
    }

    private fun applyInheritancePolicy(draft: DraftWallet) {
        _state.update {
            it.copy(
                inheritancePolicy = draft.onChainInheritancePolicy,
                validationErrors = draft.validationErrors,
                inheritanceCards = draft.inheritanceCards,
                draftSigners = draft.signers,
                isDraftFinalized = draft.finalized,
            )
        }
        rebuildDraftCards()
    }

    /** Joins the draft's cards to its signers, preferring the app's own copy of each key. */
    private fun rebuildDraftCards() {
        _state.update { state ->
            if (!state.isConfigurableInheritance) return@update state.copy(draftCards = emptyList())
            state.copy(
                draftCards = buildDraftSetupCards(
                    cards = state.inheritanceCards,
                    signers = state.draftSigners,
                    localSigner = { xfp ->
                        state.signers.firstOrNull { it.fingerPrint.equals(xfp, ignoreCase = true) }
                    },
                )
            )
        }
    }

    /**
     * Add (or Change) on a configurable-draft card. A spending card fills its next account; a
     * primary slot of a customized policy, and every fallback slot, first offers the keys already in
     * this setup (KEY-01a).
     */
    fun onDraftSlotAddClicked(card: DraftSetupCard, slot: DraftSetupSlot) {
        val offersChoice = card.section == InheritanceSection.FALLBACK ||
            (card.section == InheritanceSection.PRIMARY && _state.value.isPrimaryCustomized)
        if (offersChoice && chooseKeyOptions(card.card).isNotEmpty()) {
            _state.update { it.copy(chooseKeyTarget = ChooseKeyTarget(card.card, slot.keyIndex)) }
            return
        }
        startDraftDeviceAdd(card, slot)
    }

    /** KEY-01a "Add key": the regular add-key flow for this slot. */
    fun onChooseKeyAddNew() {
        val (card, slot) = takeChooseKeyTarget() ?: return
        startDraftDeviceAdd(card, slot)
    }

    private fun takeChooseKeyTarget(): Pair<DraftSetupCard, DraftSetupSlot>? {
        val target = _state.value.chooseKeyTarget ?: return null
        _state.update { it.copy(chooseKeyTarget = null) }
        val card = _state.value.draftCards.firstOrNull { it.card == target.card } ?: return null
        val slot = card.slots.firstOrNull { it.keyIndex == target.keyIndex } ?: return null
        return card to slot
    }

    /** Cancel keeps whatever the slot held: nothing is sent until a key is picked. */
    fun dismissChooseKey() {
        _state.update { it.copy(chooseKeyTarget = null) }
    }

    /**
     * Keys already in this setup, one per device, for the choose-key sheet of a slot on [card]. A key
     * already in that route (the slot's own included) is left out: one route can't use the same key
     * twice. A customized primary comes as one card per key, so the route is every card of the same
     * section and fallback.
     */
    fun chooseKeyOptions(card: DraftInheritanceCard): List<SignerModel> {
        val cards = _state.value.draftCards
        val inThisRoute = cards
            .filter { it.card.section == card.section && it.card.fallbackIndex == card.fallbackIndex }
            .flatMap { it.slots }
            .mapNotNull { it.signer?.fingerPrint?.lowercase() }
            .toSet()
        return cards
            .filterNot { it.isServerKey }
            .flatMap { it.slots.mapNotNull { slot -> slot.signer } }
            .filterNot { it.fingerPrint.lowercase() in inThisRoute }
            .distinctBy { it.fingerPrint.lowercase() }
    }

    /**
     * KEY-01a "Choose key": the key goes straight into the slot and the setup card comes back. Only
     * "Add key" runs the inheritance guide (KEY-03 → KEY-04 → KEY-02).
     */
    fun onChooseKeySelected(xfp: String) {
        val (card, slot) = takeChooseKeyTarget() ?: return
        importChosenKey(card, slot, xfp)
    }

    /** An inheritance add came back from the key picker, so its guide ran (or was not needed). */
    fun onDraftSignerIntroCompleted() {
        val slot = draftKeySlotHolder.slotFor(groupId) ?: return
        if (slot.isInheritance) markInheritanceGuideSeen(cardOfKey(slot.keyIndex)?.card)
    }

    private fun cardOfKey(keyIndex: Int): DraftSetupCard? =
        _state.value.draftCards.firstOrNull { card -> card.slots.any { it.keyIndex == keyIndex } }

    /** The primary guide runs once per setup; a fallback's guide once per fallback ([card] null: primary). */
    private fun markInheritanceGuideSeen(card: DraftInheritanceCard?) {
        val fallbackIndex = card?.fallbackIndex?.takeIf { card.section == InheritanceSection.FALLBACK }
        if (fallbackIndex == null) {
            savedStateHandle[KEY_INHERITANCE_GUIDE_SEEN] = true
            _state.update { it.copy(isInheritanceGuideSeenInSession = true) }
            return
        }
        val seen = _state.value.fallbackGuidesSeenInSession + fallbackIndex
        savedStateHandle[KEY_FALLBACK_GUIDES_SEEN] = seen.toIntArray()
        _state.update { it.copy(fallbackGuidesSeenInSession = seen) }
    }

    /** KEY-03 / KEY-05 copy for a key going into [card]. */
    private fun inheritanceGuide(card: DraftInheritanceCard): OnChainInheritanceGuide = when {
        card.section == InheritanceSection.FALLBACK -> OnChainInheritanceGuide.FALLBACK
        _state.value.isPrimaryCustomized -> OnChainInheritanceGuide.PRIMARY
        else -> OnChainInheritanceGuide.DEFAULT_PRIMARY
    }

    /**
     * Put [xfp]'s account for this slot (`account_index_by_xfp`) in the slot. Uses the account the app
     * or the draft already holds; otherwise reads it off that device (no device-safety notice: the
     * device is already in this setup).
     */
    private fun importChosenKey(card: DraftSetupCard, slot: DraftSetupSlot, xfp: String) {
        val draftSlot = card.slotToAdd(groupId, slot)
        val account = draftSlot.expectedAccount(xfp) ?: run {
            viewModelScope.launch { _event.emit(AddKeyListEvent.ShowErrorRes(R.string.nc_draft_key_account_unavailable)) }
            return
        }
        val device = chooseKeyOptions(card.card).firstOrNull { it.fingerPrint.equals(xfp, ignoreCase = true) } ?: return
        importDraftAccount(card, slot, device, account)
    }

    /** [xfp]'s [account] as the app or the draft already knows it, xpub included. */
    private fun findAccount(xfp: String, account: Int): SingleSigner? {
        singleSigners.firstOrNull {
            it.masterFingerprint.equals(xfp, ignoreCase = true) &&
                bip32AccountIndex(it.derivationPath) == account && it.xpub.isNotEmpty()
        }?.let { return it }
        return _state.value.draftSigners.firstOrNull {
            it.xfp.equals(xfp, ignoreCase = true) &&
                bip32AccountIndex(it.derivationPath.orEmpty()) == account && !it.xpub.isNullOrEmpty()
        }?.let { server ->
            val serverXfp = server.xfp.orEmpty().lowercase()
            // A TAPSIGNER's card details are read through its master signer (id = xfp); without the card
            // on this phone it has to be tapped (importTapSignerAccount).
            val masterSignerId = masterSigners.firstOrNull { it.id.equals(serverXfp, ignoreCase = true) }?.id.orEmpty()
            if (server.type == SignerType.NFC && masterSignerId.isEmpty()) return null
            SingleSigner(
                name = server.name.orEmpty(),
                xpub = server.xpub.orEmpty(),
                derivationPath = server.derivationPath.orEmpty(),
                masterFingerprint = serverXfp,
                masterSignerId = masterSignerId,
                type = server.type,
                tags = server.tags.mapNotNull { tag -> SignerTag.entries.firstOrNull { it.name == tag } },
            )
        }
    }

    /**
     * Runs the add-key dispatch for one empty (or, when customized, changed) slot. A spending card
     * whose device is already known reads the slot's account off that device; otherwise the key
     * picker runs with the slot's account, the inheritance guide once per setup, and "Before you
     * use this device" for the new device.
     */
    private fun startDraftDeviceAdd(card: DraftSetupCard, slot: DraftSetupSlot) {
        val account = slot.accountIndex ?: 0
        val sameDevice = card.device.takeIf { card.section == InheritanceSection.SPENDING }
        if (sameDevice != null) {
            importDraftAccount(card, slot, sameDevice, account)
            return
        }
        val isInheritance = card.section != InheritanceSection.SPENDING
        startDraftSlot(card, slot, account)
        val step = dispatchStep(isInheritance, account)
        setCurrentStep(step)
        val options = DraftAddOptions(
            accountIndex = account,
            isInheritance = isInheritance,
            inheritanceGuide = inheritanceGuide(card.card),
            fallbackNumber = card.card.fallbackNumber,
            showInheritanceGuide = isInheritance && !_state.value.isGuideSeen(card.card),
            showDeviceSafety = true,
        )
        viewModelScope.launch {
            _event.emit(AddKeyListEvent.OnAddKey(AddKeyOnChainData(steps = listOf(step)), options))
        }
    }

    /**
     * Another account of a device already in this setup: reuse it if the app or the draft holds it
     * (TAPSIGNER: derive it), else read exactly [account] off that device.
     */
    private fun importDraftAccount(card: DraftSetupCard, slot: DraftSetupSlot, device: SignerModel, account: Int) {
        val isInheritance = card.section != InheritanceSection.SPENDING
        startDraftSlot(card, slot, account)
        val step = dispatchStep(isInheritance, account)
        setCurrentStep(step)
        viewModelScope.launch {
            findAccount(device.fingerPrint, account)?.let {
                onSelectedExistingHardwareSigner(it)
                return@launch
            }
            if (device.type == SignerType.NFC) {
                importTapSignerAccount(device, account)
                return@launch
            }
            getRemoteSignerByIndex(device.fingerPrint, account).let { result ->
                if (result is Result.Success && result.data != null) {
                    onSelectedExistingHardwareSigner(result.data!!)
                    return@launch
                }
            }
            _event.emit(
                AddKeyListEvent.OpenDraftDeviceFlow(
                    data = AddKeyOnChainData(steps = listOf(step)),
                    type = device.type,
                    tag = device.tags.firstOrNull { it != SignerTag.INHERITANCE },
                    options = DraftAddOptions(
                        accountIndex = account,
                        sameDevice = device,
                        isInheritance = isInheritance,
                        inheritanceGuide = inheritanceGuide(card.card),
                        fallbackNumber = card.card.fallbackNumber,
                        showInheritanceGuide = false,
                        showDeviceSafety = false,
                    ),
                )
            )
        }
    }

    /** TAPSIGNER [account] of [device]; an uncached xpub asks for a tap first. */
    private suspend fun importTapSignerAccount(device: SignerModel, account: Int) {
        when (val result = getSignerFromMasterSignerByIndex(device.fingerPrint, account)) {
            is Result.Success -> result.data?.let { processTapSignerWithCompleteData(it, it.toModel()) }
            is Result.Error -> {
                val error = result.exception
                if (error is NCNativeException && error.message.contains("-1009")) {
                    savedStateHandle[KEY_TAPSIGNER_MASTER_ID] = device.fingerPrint
                    savedStateHandle[KEY_TAPSIGNER_PATH] = getPath(account, isTestNet)
                    savedStateHandle[KEY_TAPSIGNER_CONTEXT] = TapSignerCachingContext.HANDLE_CUSTOM_KEY_ACCOUNT_RESULT.name
                    pendingTapSignerSignerModel = device
                    requestCacheTapSignerXpub()
                } else {
                    _event.emit(AddKeyListEvent.ShowError(error.message.orUnknownError()))
                }
            }
        }
    }

    private fun dispatchStep(isInheritance: Boolean, account: Int): MembershipStep = when {
        isInheritance -> DRAFT_PRIMARY_DISPATCH_STEP
        account == 0 -> DRAFT_SPENDING_DISPATCH_STEP_ACCOUNT_0
        else -> DRAFT_SPENDING_DISPATCH_STEP_ACCOUNT_1
    }

    /** Synchronously too: the existing-key path reads the step before the saved-state flow emits. */
    private fun setCurrentStep(step: MembershipStep) {
        savedStateHandle[KEY_CURRENT_STEP] = step
        membershipStepManager.setCurrentStep(step)
    }

    private fun startDraftSlot(card: DraftSetupCard, slot: DraftSetupSlot, account: Int) {
        activeDraftAccount = account
        draftKeySlotHolder.set(card.slotToAdd(groupId, slot))
    }

    /** After an import: the next Add sets its own slot; reload the cards. */
    private fun onDraftKeySynced() {
        if (activeDraftAccount == null) return
        clearDraftSlot()
        refresh()
    }

    private fun clearDraftSlot() {
        activeDraftAccount = null
        draftKeySlotHolder.clear()
    }

    override fun onCleared() {
        clearDraftSlot()
        super.onCleared()
    }

    /**
     * Verify on a configurable-draft key. The guided default's inheritance key opens the Verify
     * backups list (VER-01); everything else, fallback keys included, runs the key's own backup check.
     */
    fun onDraftSlotVerifyClicked(card: DraftSetupCard, slot: DraftSetupSlot) {
        if (card.section == InheritanceSection.PRIMARY && !_state.value.isPrimaryCustomized) {
            _state.update { it.copy(isVerifyBackupsVisible = true) }
            return
        }
        slot.signer?.let(::verifyDraftKey)
    }

    fun dismissVerifyBackups() {
        _state.update { it.copy(isVerifyBackupsVisible = false) }
    }

    /** Runs the existing seed-phrase backup check for [signer] (TapSigner: its backup file). */
    fun verifyDraftKey(signer: SignerModel) {
        viewModelScope.launch {
            val info = membershipStepState.value.lastOrNull {
                it.masterSignerId.equals(signer.fingerPrint, ignoreCase = true)
            }
            val backUpFileName = info?.extraData?.let(::getBackUpFileName).orEmpty()
            _event.emit(
                AddKeyListEvent.OnVerifySigner(
                    signer = signer,
                    filePath = nfcFileManager.buildFilePath(info?.keyIdInServer.orEmpty()),
                    backUpFileName = backUpFileName,
                    isBackupNfc = signer.type == SignerType.NFC && backUpFileName.isEmpty(),
                )
            )
        }
    }

    fun showPrimaryPolicySheet() {
        _state.update { it.copy(isPrimaryPolicySheetVisible = true) }
    }

    fun dismissPrimaryPolicySheet() {
        _state.update { it.copy(isPrimaryPolicySheetVisible = false) }
    }

    /** PRI-02 Continue: customize the primary to any [m] of [n] keys (PUT draft-wallets/primary). */
    fun updatePrimaryPolicy(m: Int, n: Int) {
        runDraftUpdate(beforeRun = { it.copy(isPrimaryPolicySheetVisible = false) }) {
            updateDraftPrimaryInheritanceUseCase(
                UpdateDraftPrimaryInheritanceUseCase.Param(m = m, n = n, groupId = groupId)
            )
        }
    }

    /** More → Cancel customization: back to the default 2-of-4; fallbacks are kept (DELETE draft-wallets/primary). */
    fun cancelPrimaryCustomization() {
        runDraftUpdate {
            resetDraftPrimaryInheritanceUseCase(ResetDraftPrimaryInheritanceUseCase.Param(groupId = groupId))
        }
    }

    /** FBK-01a for a new fallback (its intro already ran), starting at 1 of 1. */
    fun showAddFallbackSheet() {
        _state.update { it.copy(fallbackPolicyTarget = FallbackPolicyTarget(index = null)) }
    }

    /** Edit on a fallback: the same sheet, with its current M of N. */
    fun showEditFallbackSheet(index: Int) {
        _state.update { it.copy(fallbackPolicyTarget = FallbackPolicyTarget(index = index)) }
    }

    fun dismissFallbackPolicySheet() {
        _state.update { it.copy(fallbackPolicyTarget = null) }
    }

    /**
     * FBK-01a Continue: append a fallback (POST draft-wallets/fallbacks) or change one's M of N
     * (PUT draft-wallets/fallbacks/{index}); the backend revalidates every route.
     */
    fun saveFallbackPolicy(m: Int, n: Int) {
        val target = _state.value.fallbackPolicyTarget ?: return
        runDraftUpdate(beforeRun = { it.copy(fallbackPolicyTarget = null) }) {
            if (target.index == null) {
                addDraftFallbackInheritanceUseCase(
                    AddDraftFallbackInheritanceUseCase.Param(m = m, n = n, groupId = groupId)
                )
            } else {
                updateDraftFallbackInheritanceUseCase(
                    UpdateDraftFallbackInheritanceUseCase.Param(index = target.index, m = m, n = n, groupId = groupId)
                )
            }
        }
    }

    fun requestRemoveFallback(index: Int) {
        _state.update { it.copy(removeFallbackIndex = index) }
    }

    fun dismissRemoveFallback() {
        _state.update { it.copy(removeFallbackIndex = null) }
    }

    /** FBK-10 / FBK-11: later fallbacks move down one index and keep their keys and times. */
    fun confirmRemoveFallback() {
        val index = _state.value.removeFallbackIndex ?: return
        runDraftUpdate(beforeRun = { it.copy(removeFallbackIndex = null) }) {
            removeDraftFallbackInheritanceUseCase(
                RemoveDraftFallbackInheritanceUseCase.Param(index = index, groupId = groupId)
            )
        }.invokeOnCompletion {
            // Fallback guides are tracked by index, which just shifted: forget the ones from here on.
            val seen = _state.value.fallbackGuidesSeenInSession.filter { it < index }.toSet()
            savedStateHandle[KEY_FALLBACK_GUIDES_SEEN] = seen.toIntArray()
            _state.update { it.copy(fallbackGuidesSeenInSession = seen) }
        }
    }

    /** One draft-edit call with the loading dialog, then the returned draft and a full refresh. */
    private fun runDraftUpdate(
        beforeRun: (AddKeyListState) -> AddKeyListState = { it },
        call: suspend () -> kotlin.Result<DraftWallet>,
    ): Job = viewModelScope.launch {
        _state.update { beforeRun(it).copy(isLoading = true) }
        call().onSuccess { draft ->
            applyInheritancePolicy(draft)
            refresh()
        }.onFailure {
            _event.emit(AddKeyListEvent.ShowError(it.message.orUnknownError()))
        }
        _state.update { it.copy(isLoading = false) }
    }

    fun onAddKeyClicked(data: AddKeyOnChainData) {
        clearDraftSlot()
        viewModelScope.launch {
            // Get the next step that needs to be added
            val nextStep = data.getNextStepToAdd()
            if (nextStep != null) {
                savedStateHandle[KEY_CURRENT_STEP] = nextStep
                _event.emit(AddKeyListEvent.OnAddKey(data))
            }
        }
    }

    private fun isSignerExist(masterSignerId: String) =
        membershipStepManager.isKeyExisted(masterSignerId)

    fun onVerifyClicked(data: AddKeyOnChainData) {
        viewModelScope.launch {
            // Find the first step that needs verification (has signer but needs verification)
            val stepToVerify = data.steps.firstOrNull { step ->
                val stepData = data.stepDataMap[step]
                stepData?.signer != null && stepData.verifyType == VerifyType.NONE
            } ?: data.steps.firstOrNull { step ->
                data.stepDataMap[step]?.signer != null
            } ?: return@launch

            val signer = data.getSignerForStep(stepToVerify) ?: return@launch

            savedStateHandle[KEY_CURRENT_STEP] = stepToVerify
            val stepInfo = getStepInfo(stepToVerify)
            _event.emit(
                AddKeyListEvent.OnVerifySigner(
                    signer = signer,
                    filePath = nfcFileManager.buildFilePath(stepInfo.keyIdInServer),
                    backUpFileName = getBackUpFileName(stepInfo.extraData),
                    isBackupNfc = _state.value.missingBackupKeys.contains(data) && signer.type == SignerType.NFC
                )
            )
        }
    }

    fun onContinueClicked() {
        viewModelScope.launch {
            refresh()
            _event.emit(AddKeyListEvent.OnAddAllKey)
        }
    }

    fun requestCacheTapSignerXpub() {
        _state.update { it.copy(requestCacheTapSignerXpubEvent = true) }
    }

    fun resetRequestCacheTapSignerXpub() {
        _state.update { it.copy(requestCacheTapSignerXpubEvent = false) }
    }

    fun cacheTapSignerXpub(isoDep: android.nfc.tech.IsoDep?, cvc: String) {
        val masterSignerId = savedStateHandle.get<String>(KEY_TAPSIGNER_MASTER_ID) ?: return
        val path = savedStateHandle.get<String>(KEY_TAPSIGNER_PATH) ?: return
        val contextName = savedStateHandle.get<String>(KEY_TAPSIGNER_CONTEXT) ?: return

        isoDep ?: return

        viewModelScope.launch {
            Timber.tag("tapsigner-onchain")
                .d("cacheTapSignerXpub: currentStep=${membershipStepManager.currentStep}, pendingTapSignerData=$pendingTapSignerData, masterSignerId=$masterSignerId, path=$path, context=$contextName")
            getSignerFromTapsignerMasterSignerByPathUseCase(
                GetSignerFromTapsignerMasterSignerByPathUseCase.Data(
                    isoDep = isoDep,
                    masterSignerId = masterSignerId,
                    path = path,
                    cvc = cvc
                )
            ).onSuccess { newSigner ->
                val newSignerModel = singleSignerMapper(newSigner)

                // Resume the appropriate flow based on context
                val context = TapSignerCachingContext.valueOf(contextName)
                when (context) {
                    TapSignerCachingContext.ADD_TAPSIGNER_KEY -> {
                        // Resume addTapSignerKey flow - note: this is for Acct 0, don't call handleTapSignerAcct1Addition
                        processTapSignerWithCompleteData(
                            newSigner,
                            pendingTapSignerSignerModel ?: newSignerModel,
                            pendingTapSignerData,
                            pendingTapSignerWalletId
                        )
                    }

                    TapSignerCachingContext.HANDLE_ACCT1_ADDITION -> {
                        // Resume handleTapSignerAcct1Addition flow - this is for Acct 1, don't call handleTapSignerAcct1Addition again
                        processTapSignerWithCompleteData(newSigner, newSignerModel)
                    }

                    TapSignerCachingContext.HANDLE_CUSTOM_KEY_ACCOUNT_RESULT -> {
                        // Resume handleCustomKeyAccountResult flow
                        processTapSignerWithCompleteData(newSigner, newSignerModel)
                    }
                }

                // Clear context
                clearTapSignerCachingContext()
            }.onFailure { error ->
                _event.emit(AddKeyListEvent.ShowError(error.message.orUnknownError()))
                clearTapSignerCachingContext()
            }

            resetRequestCacheTapSignerXpub()
        }
    }

    private fun clearTapSignerCachingContext() {
        savedStateHandle.remove<String>(KEY_TAPSIGNER_MASTER_ID)
        savedStateHandle.remove<String>(KEY_TAPSIGNER_PATH)
        savedStateHandle.remove<String>(KEY_TAPSIGNER_CONTEXT)
        pendingTapSignerData = null
        pendingTapSignerWalletId = null
        pendingTapSignerSignerModel = null
    }

    fun addExistingTapSignerKey(
        signerModel: SignerModel,
        data: AddKeyOnChainData? = null,
        walletId: String? = null
    ) {
        viewModelScope.launch {
            Timber.tag("tapsigner-onchain")
                .d("addExistingTapSignerKey: currentStep=${membershipStepManager.currentStep}, data=$data, signerModel=$signerModel, walletId=$walletId")
            // A configurable-draft slot needs one specific account, not the next unused one.
            activeDraftAccount?.let { account ->
                if (signerModel.type == SignerType.NFC) {
                    importTapSignerAccount(signerModel, account)
                    return@launch
                }
            }
            if (signerModel.isMasterSigner && signerModel.type == SignerType.NFC) {
                var masterSigner = masterSigners.find { it.id == signerModel.fingerPrint }
                if (masterSigner == null) {
                    loadSigners()
                    masterSigner = masterSigners.find { it.id == signerModel.fingerPrint }
                }
                if (masterSigner == null) {
                    _event.emit(AddKeyListEvent.ShowError("Master signer not found with id=${signerModel.fingerPrint}"))
                    return@launch
                }

                // Use GetUnusedSignerFromMasterSignerV2UseCase to get signer with complete data
                getUnusedSignerFromMasterSignerV2UseCase(
                    GetUnusedSignerFromMasterSignerV2UseCase.Params(
                        masterSigner,
                        WalletType.MULTI_SIG,
                        AddressType.NATIVE_SEGWIT
                    )
                ).onSuccess { singleSigner ->
                    // Process the signer with complete data
                    processTapSignerWithCompleteData(singleSigner, signerModel, data, walletId)
                }.onFailure { error ->
                    if (error is NCNativeException && error.message.contains("-1009")) {
                        // Store context for TapSigner caching - using index 0 for first account
                        savedStateHandle[KEY_TAPSIGNER_MASTER_ID] = signerModel.fingerPrint
                        savedStateHandle[KEY_TAPSIGNER_PATH] = getPath(0, isTestNet)
                        savedStateHandle[KEY_TAPSIGNER_CONTEXT] =
                            TapSignerCachingContext.ADD_TAPSIGNER_KEY.name
                        pendingTapSignerData = data
                        pendingTapSignerWalletId = walletId
                        pendingTapSignerSignerModel = signerModel
                        requestCacheTapSignerXpub()
                    } else {
                        _event.emit(AddKeyListEvent.ShowError(error.message.orUnknownError()))
                    }
                }
            } else {
                _event.emit(AddKeyListEvent.ShowError("Invalid signer type for TapSigner addition"))
            }
        }
    }

    private suspend fun processTapSignerWithCompleteData(
        signer: SingleSigner,
        signerModel: SignerModel,
        data: AddKeyOnChainData? = null,
        walletId: String? = null
    ) {
        Timber.tag("tapsigner-onchain")
            .d("processTapSignerWithCompleteData: currentStep=${membershipStepManager.currentStep}, data=$data, signer=$signer, signerModel=$signerModel, walletId=$walletId")
        // Sync the signer to the membership system
        syncKeyUseCase(
            SyncKeyUseCase.Param(
                step = membershipStepManager.currentStep
                    ?: throw IllegalArgumentException("Current step empty"),
                signer = signer,
                walletType = WalletType.MINISCRIPT,
                groupId = groupId
            )
        ).onFailure {
            _event.emit(it.toAddKeyErrorEvent())
            return
        }

        val currentStep = membershipStepManager.currentStep
            ?: throw IllegalArgumentException("Current step empty")
        val verifyType = getVerificationTypeForSigner(signerModel, data)
        // Save membership step
        saveMembershipStepUseCase(
            MembershipStepInfo(
                step = currentStep,
                masterSignerId = signer.masterFingerprint,
                plan = membershipStepManager.localMembershipPlan,
                verifyType = verifyType,
                extraData = gson.toJson(
                    SignerExtra(
                        derivationPath = signer.derivationPath,
                        isAddNew = false,
                        signerType = signer.type,
                        userKeyFileName = ""
                    )
                ),
                groupId = groupId
            )
        )

        // Update the card with the new signer for the current step
        updateCardForStep(currentStep, signerModel, verifyType)
        onDraftKeySynced()

        // After successfully adding signer, handle TapSigner Acct 1 addition if we have the required data
        val nextStep = data?.getNextStepToAdd(currentStep)
        if (nextStep != null && walletId != null) {
            savedStateHandle[KEY_CURRENT_STEP] = nextStep
            membershipStepManager.setCurrentStep(nextStep)
            handleTapSignerAcct1Addition(data, signerModel, walletId)
        }
    }

    fun handleTapSignerAcct1Addition(
        data: AddKeyOnChainData,
        firstSigner: SignerModel,
        walletId: String
    ) {
        viewModelScope.launch {
            Timber.tag("tapsigner-onchain")
                .d("handleTapSignerAcct1Addition: currentStep=${membershipStepManager.currentStep}, data=$data, firstSigner=$firstSigner, walletId=$walletId")
            // Try to get signer at index 1
            val signerByIndexResult =
                getSignerFromMasterSignerByIndex(firstSigner.fingerPrint, 1)

            when (signerByIndexResult) {
                is Result.Success -> {
                    // Successfully got signer at index 1, process it with complete data
                    signerByIndexResult.data?.let { singleSigner ->
                        processTapSignerWithCompleteData(
                            singleSigner,
                            singleSigner.toModel()
                        )
                    }
                }

                is Result.Error -> {
                    val error = signerByIndexResult.exception
                    if (error is NCNativeException && error.message.contains("-1009")) {
                        // Store context for TapSigner caching - using index 1 for second account
                        savedStateHandle[KEY_TAPSIGNER_MASTER_ID] =
                            firstSigner.fingerPrint
                        savedStateHandle[KEY_TAPSIGNER_PATH] = getPath(1, isTestNet)
                        savedStateHandle[KEY_TAPSIGNER_CONTEXT] =
                            TapSignerCachingContext.HANDLE_ACCT1_ADDITION.name
                        pendingTapSignerData = data
                        pendingTapSignerWalletId = walletId
                        pendingTapSignerSignerModel = firstSigner
                        requestCacheTapSignerXpub()
                    } else {
                        // Other error, show error message
                        _event.emit(
                            AddKeyListEvent.ShowError(
                                error.message ?: "Failed to get signer at index 1"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun getStepInfo(step: MembershipStep): MembershipStepInfo {
        return if (isGroupWallet) {
            membershipStepState.value.find { it.step == step } ?: run {
                MembershipStepInfo(step = step, groupId = groupId)
            }
        } else {
            membershipStepState.value.filter {
                it.plan == membershipStepManager.localMembershipPlan
            }.find { it.step == step } ?: run {
                MembershipStepInfo(
                    step = step,
                    plan = membershipStepManager.localMembershipPlan,
                    groupId = ""
                )
            }
        }
    }

    fun getHardwareSigners(tag: SignerTag): List<SignerModel> {
        return if (isGroupWallet) {
            _state.value.signers.filter {
                it.type == SignerType.HARDWARE && it.tags.contains(tag)
            }
        } else {
            _state.value.signers.filter {
                isSignerExist(it.fingerPrint).not() && it.type == SignerType.HARDWARE && it.tags.contains(
                    tag
                )
            }
        }
    }

    private fun getBackUpFileName(extra: String): String {
        return runCatching {
            gson.fromJson(extra, SignerExtra::class.java).userKeyFileName
        }.getOrDefault("")
    }

    private fun getVerificationTypeForSigner(signer: SignerModel, data: AddKeyOnChainData?): VerifyType {
        return if (data?.isInheritanceKey() == true || signer.type == SignerType.NFC) {
            VerifyType.NONE
        } else {
            VerifyType.APP_VERIFIED
        }
    }

    /**
     * Gets a signer from master signer by specific index (for TapSigner flows)
     */
    suspend fun getSignerFromMasterSignerByIndex(
        fingerPrint: String,
        index: Int
    ): Result<SingleSigner?> {
        return runCatching {
            getSignerFromMasterSignerByIndexUseCase(
                GetSignerFromMasterSignerByIndexUseCase.Param(
                    masterSignerId = fingerPrint,
                    index = index,
                    walletType = WalletType.MULTI_SIG,
                    addressType = AddressType.NATIVE_SEGWIT
                )
            ).getOrThrow()
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it as Exception) }
        )
    }

    /**
     * Gets a remote signer by specific index using derivation path
     */
    suspend fun getRemoteSignerByIndex(
        fingerPrint: String,
        index: Int
    ): Result<SingleSigner?> {
        return runCatching {
            val derivationPath = getPath(index, isTestNet)
            getRemoteSignerUseCase(
                GetRemoteSignerUseCase.Data(
                    id = fingerPrint,
                    derivationPath = derivationPath
                )
            ).getOrThrow()
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it as Exception) }
        )
    }

    /**
     * Handles the logic for checking signer index and navigating accordingly
     * @param firstSigner The first signer to check
     * @param walletId The wallet ID for navigation
     */
    fun handleSignerIndexCheck(
        data: AddKeyOnChainData,
        firstSigner: SignerModel,
        walletId: String
    ) {
        viewModelScope.launch {
            // Call GetRemoteSignerUseCase
            val signerResult =
                getRemoteSignerByIndex(firstSigner.fingerPrint, 1)

            when (signerResult) {
                is Result.Success -> {
                    val signer = signerResult.data

                    if (signer != null) {
                        // If signer != null, add signer to corresponding AddKeyOnChainData.signers
                        onSelectedExistingHardwareSigner(signer, data)
                    } else {
                        // If signer == null, run handleSignerTypeLogic flow
                        _event.emit(
                            AddKeyListEvent.HandleSignerTypeLogic(
                                firstSigner.type,
                                firstSigner.tags.firstOrNull { it != SignerTag.INHERITANCE }
                            )
                        )
                    }
                }

                is Result.Error -> {
                    // If error getting signer, run handleSignerTypeLogic flow
                    _event.emit(
                        AddKeyListEvent.HandleSignerTypeLogic(
                            firstSigner.type,
                            firstSigner.tags.firstOrNull { it != SignerTag.INHERITANCE }
                        )
                    )
                }
            }
        }
    }

    private fun getPath(
        index: Int,
        isTestNet: Boolean = false,
        isMultisig: Boolean = true
    ): String {
        if (isMultisig) {
            return if (isTestNet) "m/48h/1h/${index}h/2h" else "m/48h/0h/${index}h/2h"
        }
        return if (isTestNet) "m/84h/1h/${index}h" else "m/84h/0h/${index}h"
    }

    companion object {
        /**
         * Stand-in steps for the device flows of a configurable draft; the slot index comes from
         * [DraftKeySlotHolder]. Spending accounts must not look like an inheritance key (intro
         * copy, verification), and every inheritance slot must.
         */
        private val DRAFT_SPENDING_DISPATCH_STEP_ACCOUNT_0 = MembershipStep.HONEY_ADD_HARDWARE_KEY_1_TIMELOCK
        private val DRAFT_SPENDING_DISPATCH_STEP_ACCOUNT_1 = MembershipStep.HONEY_ADD_HARDWARE_KEY_1
        private val DRAFT_PRIMARY_DISPATCH_STEP = MembershipStep.HONEY_ADD_INHERITANCE_KEY_TIMELOCK
        private const val KEY_CURRENT_STEP = "current_step"
        private const val KEY_ACTIVE_DRAFT_ACCOUNT = "active_draft_account"
        private const val KEY_INHERITANCE_GUIDE_SEEN = "inheritance_guide_seen"
        private const val KEY_FALLBACK_GUIDES_SEEN = "fallback_guides_seen"
        private const val KEY_TAPSIGNER_MASTER_ID = "tapsigner_master_id"
        private const val KEY_TAPSIGNER_PATH = "tapsigner_path"
        private const val KEY_TAPSIGNER_CONTEXT = "tapsigner_context"
    }

    private enum class TapSignerCachingContext {
        ADD_TAPSIGNER_KEY,
        HANDLE_ACCT1_ADDITION,
        HANDLE_CUSTOM_KEY_ACCOUNT_RESULT
    }
}

/** The slot the choose-key sheet fills: the card plus the slot's `key_index`. */
data class ChooseKeyTarget(val card: DraftInheritanceCard, val keyIndex: Int)

/** The fallback the policy sheet (FBK-01a) edits; [index] null adds a new one. */
data class FallbackPolicyTarget(val index: Int?)

/** 1-based number of a FALLBACK card ("Fallback 1"); 0 for any other card. */
val DraftInheritanceCard.fallbackNumber: Int
    get() = if (section == InheritanceSection.FALLBACK) (fallbackIndex ?: 0) + 1 else 0

/**
 * How the fragment starts a device flow for a configurable-draft slot.
 *
 * @property accountIndex the account the slot needs (not "how many accounts the card holds").
 * @property sameDevice set when the account must come off a device already in this setup.
 */
data class DraftAddOptions(
    val accountIndex: Int,
    val sameDevice: SignerModel? = null,
    val isInheritance: Boolean,
    val inheritanceGuide: OnChainInheritanceGuide,
    /** 1-based, for a fallback slot (KEY-05 "Your Fallback N key"); 0 otherwise. */
    val fallbackNumber: Int = 0,
    val showInheritanceGuide: Boolean,
    val showDeviceSafety: Boolean,
)

sealed class AddKeyListEvent {
    /** [draftOptions] is set for a configurable-draft slot. */
    data class OnAddKey(val data: AddKeyOnChainData, val draftOptions: DraftAddOptions? = null) : AddKeyListEvent()
    data class OnVerifySigner(
        val signer: SignerModel,
        val filePath: String,
        val backUpFileName: String,
        val isBackupNfc: Boolean = false
    ) : AddKeyListEvent()

    data object OnAddAllKey : AddKeyListEvent()
    data object OnKeyVerified : AddKeyListEvent()
    data object SelectAirgapType : AddKeyListEvent()
    data object RequireReopenWallet : AddKeyListEvent()
    data class HandleSignerTypeLogic(val type: SignerType, val tag: SignerTag?) : AddKeyListEvent()
    data class ShowError(val message: String) : AddKeyListEvent()
    data class ShowErrorRes(@StringRes val messageRes: Int, val args: List<Any> = emptyList()) : AddKeyListEvent()

    /** Run [type]'s device flow for a configurable-draft slot described by [data]. */
    data class OpenDraftDeviceFlow(
        val data: AddKeyOnChainData,
        val type: SignerType,
        val tag: SignerTag?,
        val options: DraftAddOptions,
    ) : AddKeyListEvent()

    /** Run only the inheritance guide (KEY-03/04) before a key picked from this setup is used. */
    data class UpdateSignerTag(val signer: SignerModel) : AddKeyListEvent()
}

data class AddKeyListState(
    val isLoading: Boolean = false,
    val isRefresh: Boolean = false,
    val signers: List<SignerModel> = emptyList(),
    val walletType: WalletType? = null,
    val isCustomized: Boolean = false,
    val groupWalletType: GroupWalletType? = null,
    val requestCacheTapSignerXpubEvent: Boolean = false,
    val missingBackupKeys: List<AddKeyOnChainData> = emptyList(),
    val shouldShowKeyAdded: Boolean = false,
    /** `onchain_inheritance_policy` of the draft; null for a draft the server built without one. */
    val inheritancePolicy: OnChainInheritancePolicy? = null,
    val validationErrors: List<InheritanceValidationError> = emptyList(),
    val isPrimaryPolicySheetVisible: Boolean = false,
    val inheritanceCards: List<DraftInheritanceCard> = emptyList(),
    val draftSigners: List<SignerServer> = emptyList(),
    /** Backend readiness (`finalized`). */
    val isDraftFinalized: Boolean = false,
    /** Cards of a configurable draft, joined to their keys, in the server's order. */
    val draftCards: List<DraftSetupCard> = emptyList(),
    /** The slot the choose-key sheet (KEY-01a) fills; null when the sheet is hidden. */
    val chooseKeyTarget: ChooseKeyTarget? = null,
    val isVerifyBackupsVisible: Boolean = false,
    val isInheritanceGuideSeenInSession: Boolean = false,
    /** Fallback indices whose guide (KEY-05 / KEY-06) ran in this session. */
    val fallbackGuidesSeenInSession: Set<Int> = emptySet(),
    /** The fallback policy sheet (FBK-01a); null when hidden. */
    val fallbackPolicyTarget: FallbackPolicyTarget? = null,
    /** The fallback the remove confirmation (FBK-10 / FBK-11) is for; null when hidden. */
    val removeFallbackIndex: Int? = null,
) {
    /**
     * The inheritance guide runs once per wallet setup: it ran in this session, or a primary key is
     * already in (adding one runs it).
     */
    val isInheritanceGuideSeen: Boolean
        get() = isInheritanceGuideSeenInSession || primarySlots.any { it.isFilled }

    /**
     * Whether a key going into [card] still needs its guide: the primary guide runs once per setup,
     * a fallback's once per fallback (or not at all once that fallback holds a key).
     */
    fun isGuideSeen(card: DraftInheritanceCard): Boolean {
        if (card.section != InheritanceSection.FALLBACK) return isInheritanceGuideSeen
        val index = card.fallbackIndex ?: return true
        return index in fallbackGuidesSeenInSession || fallbackSlots(index).any { it.isFilled }
    }

    val spendingCards: List<DraftSetupCard>
        get() = draftCards.filter { it.section == InheritanceSection.SPENDING }

    /** The draft's fallbacks in order (FALLBACK cards join them by `fallback_index`). */
    val fallbacks: List<FallbackInheritanceCondition>
        get() = if (isConfigurableInheritance) inheritancePolicy?.fallbacks.orEmpty().sortedBy { it.index } else emptyList()

    fun fallbackCards(index: Int): List<DraftSetupCard> =
        draftCards.filter { it.section == InheritanceSection.FALLBACK && it.card.fallbackIndex == index }

    fun fallbackSlots(index: Int): List<DraftSetupSlot> = fallbackCards(index).flatMap { it.slots }

    /** Shown under a fallback's card. */
    fun fallbackValidationErrors(index: Int): List<InheritanceValidationError> =
        validationErrors.filter { it.section == InheritanceSection.FALLBACK && it.fallbackIndex == index }

    val primaryCards: List<DraftSetupCard>
        get() = draftCards.filter { it.section == InheritanceSection.PRIMARY }

    /** The separate inheritance key(s) of the guided default, or the customized primary's keys. */
    val primarySlots: List<DraftSetupSlot>
        get() = primaryCards.flatMap { it.slots }

    /** One entry per owner device on the spending cards (Verify backups: "Other keys in this wallet"). */
    val ownerKeys: List<DraftSetupSlot>
        get() = spendingCards.filterNot { it.isServerKey }
            .mapNotNull { card ->
                val signer = card.device ?: return@mapNotNull null
                val verified = card.slots.any { it.isVerified }
                card.slots.first { it.signer != null }.copy(
                    signer = signer,
                    verifyType = if (verified) VerifyType.APP_VERIFIED else VerifyType.NONE,
                )
            }
            .distinctBy { it.signer?.fingerPrint?.lowercase() }

    /**
     * The guided default needs the inheritance key plus at least one owner key verified (the key
     * shared with the Beneficiary); the backend's `finalized` covers the customized policy. Every
     * fallback key that is in must be verified too (FBK-03: Verify before Continue).
     */
    val isRequiredVerificationDone: Boolean
        get() = (isPrimaryCustomized || (primarySlots.all { it.isVerified } && ownerKeys.any { it.isVerified })) &&
            fallbacks.all { fallback -> fallbackSlots(fallback.index).filter { it.isFilled }.all { it.isVerified } }

    /**
     * The guided default keeps Verify on its inheritance key while any key on Verify backups (the
     * inheritance key or an owner key) is unverified: it is the only way back into that screen.
     */
    val hasUnverifiedBackup: Boolean
        get() = primarySlots.any { it.isFilled && !it.isVerified } || ownerKeys.any { !it.isVerified }

    /** Continue on a configurable draft: backend-ready, no setup issue, and the verification above. */
    val isDraftReadyToContinue: Boolean
        get() = isDraftFinalized && validationErrors.isEmpty() && isRequiredVerificationDone

    /** Primary customization, fallbacks and backend validation apply only to a CONFIGURABLE draft. */
    val isConfigurableInheritance: Boolean
        get() = inheritancePolicy?.isConfigurable == true

    val isPrimaryCustomized: Boolean
        get() = isConfigurableInheritance && inheritancePolicy?.primary?.isCustomized == true

    /** Shown under the primary inheritance card. */
    val primaryValidationErrors: List<InheritanceValidationError>
        get() = validationErrors.filter { it.section == InheritanceSection.PRIMARY }

    /**
     * Issues no card shows (spending, or a fallback the draft no longer lists), at the end of the
     * list; never dropped, because each one still blocks Continue.
     */
    val otherValidationErrors: List<InheritanceValidationError>
        get() {
            val fallbackIndices = fallbacks.map { it.index }.toSet()
            return validationErrors.filter {
                it.section != InheritanceSection.PRIMARY &&
                    !(it.section == InheritanceSection.FALLBACK && it.fallbackIndex in fallbackIndices)
            }
        }
}
