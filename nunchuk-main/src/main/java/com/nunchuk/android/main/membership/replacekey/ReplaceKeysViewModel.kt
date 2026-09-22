package com.nunchuk.android.main.membership.replacekey

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.CreateWallet2UseCase
import com.nunchuk.android.core.domain.membership.SyncPersonalWallets
import com.nunchuk.android.core.domain.utils.NfcFileManager
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.mapper.SingleSignerMapper
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.isRecommendedMultiSigPath
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.main.membership.key.TapSignerKeyResolver
import com.nunchuk.android.main.membership.model.InheritanceClaimState
import com.nunchuk.android.main.membership.model.toInheritanceClaimState
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.ByzantineMember
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.StateEvent
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.inheritance.ClaimOption
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.model.signer.SignerServer
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.GetIndexFromPathUseCase
import com.nunchuk.android.usecase.UpdateRemoteSignerUseCase
import com.nunchuk.android.usecase.UpdateWalletUseCase
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
import com.nunchuk.android.usecase.byzantine.SyncGroupWalletUseCase
import com.nunchuk.android.usecase.byzantine.SyncGroupWalletsUseCase
import com.nunchuk.android.usecase.membership.SetReplaceKeyVerifiedUseCase
import com.nunchuk.android.usecase.replace.FinalizeReplaceKeyUseCase
import com.nunchuk.android.usecase.replace.GetReplaceWalletStatusUseCase
import com.nunchuk.android.usecase.replace.InitReplaceKeyUseCase
import com.nunchuk.android.usecase.replace.ReplaceKeyUseCase
import com.nunchuk.android.usecase.replace.ResetReplaceKeyUseCase
import com.nunchuk.android.usecase.signer.GetAllSignersUseCase
import com.nunchuk.android.usecase.signer.RemoveKeyReplacementUseCase
import com.nunchuk.android.usecase.wallet.GetServerWalletUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class ReplaceKeysViewModel @Inject constructor(
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val createWallet2UseCase: CreateWallet2UseCase,
    private val getGroupUseCase: GetGroupUseCase,
    private val accountManager: AccountManager,
    private val getReplaceWalletStatusUseCase: GetReplaceWalletStatusUseCase,
    private val finalizeReplaceKeyUseCase: FinalizeReplaceKeyUseCase,
    private val initReplaceKeyUseCase: InitReplaceKeyUseCase,
    private val getAllSignersUseCase: GetAllSignersUseCase,
    private val masterSignerMapper: MasterSignerMapper,
    private val replaceKeyUseCase: ReplaceKeyUseCase,
    private val updateRemoteSignerUseCase: UpdateRemoteSignerUseCase,
    private val resetReplaceKeyUseCase: ResetReplaceKeyUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val nfcFileManager: NfcFileManager,
    private val singleSignerMapper: SingleSignerMapper,
    private val syncPersonalWallets: SyncPersonalWallets,
    private val syncGroupWalletsUseCase: SyncGroupWalletsUseCase,
    private val syncGroupWalletUseCase: SyncGroupWalletUseCase,
    private val getServerWalletUseCase: GetServerWalletUseCase,
    private val pushEventManager: PushEventManager,
    assistedWalletManager: AssistedWalletManager,
    private val getIndexFromPathUseCase: GetIndexFromPathUseCase,
    private val updateWalletUseCase: UpdateWalletUseCase,
    private val ncDataStore: NcDataStore,
    private val removeKeyReplacementUseCase: RemoveKeyReplacementUseCase,
    private val setReplaceKeyVerifiedUseCase: SetReplaceKeyVerifiedUseCase,
    private val tapSignerKeyResolver: TapSignerKeyResolver,
) : ViewModel() {
    private val args = ReplaceKeysFragmentArgs.fromSavedStateHandle(savedStateHandle)
    val isActiveAssistedWallet: Boolean by lazy {
        assistedWalletManager.isActiveAssistedWallet(args.walletId)
    }
    private val _uiState =
        MutableStateFlow(ReplaceKeysUiState(isActiveAssistedWallet = isActiveAssistedWallet))
    val uiState = _uiState.asStateFlow()
    private val singleSigners = mutableListOf<SingleSigner>()
    private val replacedSigners = mutableListOf<SignerServer>()

    private var loadWalletStatusJob: Job? = null

    /**
     * The slot whose replacement inheritance key is waiting for its sharing method to be picked.
     * Null once the prompt has been raised, or when nothing is pending.
     */
    private var promptClaimOptionsForSlot: String? = null

    /**
     * Whatever occupied that slot when the picker opened. Air-gap and Coldcard perform the replace
     * inside their own screens and never hand a key back, so the prompt cannot wait for a result —
     * it waits for the replacement status to show a key that was not there before.
     */
    private var promptClaimOptionsPreviousXfp: String? = null

    init {
        viewModelScope.launch {
            pushEventManager.event.collect {
                if (it is PushEvent.ReplaceKeyChange) {
                    getReplaceWalletStatus()
                }
            }
        }
        if (!isActiveAssistedWallet) {
            viewModelScope.launch {
                pushEventManager.event.filterIsInstance<PushEvent.LocalUserSignerAdded>()
                    .collect {
                        replaceFreeWalletSigner(it.signer)
                    }
            }
        }
        viewModelScope.launch {
            getWalletDetail2UseCase(args.walletId).onSuccess { wallet ->
                _uiState.update {
                    it.copy(
                        walletSigners = wallet.signers.filter { signer -> signer.type != SignerType.SERVER }
                            .map { signer ->
                                singleSignerMapper(signer)
                            },
                        isMultiSig = wallet.signers.size > 1,
                        addressType = wallet.addressType
                    )
                }
            }
        }
        if (isActiveAssistedWallet) {
            if (args.groupId.isNotEmpty()) {
                viewModelScope.launch {
                    getGroupUseCase(
                        GetGroupUseCase.Params(args.groupId)
                    ).map { it.getOrThrow() }
                        .distinctUntilChanged()
                        .collect { group ->
                            _uiState.update {
                                it.copy(group = group, myRole = currentUserRole(group.members))
                            }
                        }
                }
                viewModelScope.launch {
                    syncGroupWalletUseCase(args.groupId).onSuccess { wallet ->
                        _uiState.update {
                            it.copy(inheritanceXfps = wallet.signers.filter { signer ->
                                signer.tags.contains(
                                    SignerTag.INHERITANCE.name
                                )
                            }.mapNotNull { signer -> signer.xfp }.toSet())
                        }
                    }
                }
            } else {
                viewModelScope.launch {
                    getServerWalletUseCase(args.walletId).onSuccess { wallet ->
                        _uiState.update {
                            it.copy(inheritanceXfps = wallet.signers.filter { signer ->
                                signer.tags.contains(
                                    SignerTag.INHERITANCE.name
                                )
                            }.mapNotNull { signer -> signer.xfp }.toSet())
                        }
                    }
                }
            }
            getReplaceWalletStatus()
        } else {
            _uiState.update { it.copy(isDataLoaded = true) }
        }
        loadSigners()
    }

    private fun loadSigners() {
        viewModelScope.launch {
            getAllSignersUseCase(false).onSuccess { pair ->
                val singleSigner = pair.second.distinctBy { it.masterFingerprint }
                singleSigners.apply {
                    clear()
                    addAll(singleSigner)
                }
                val signers = pair.first.map { signer ->
                    masterSignerMapper(signer)
                } + singleSigner.map { signer -> signer.toModel() }
                _uiState.update { it.copy(signers = signers) }
            }
        }
    }

    fun getReplaceWalletStatus() {
        if (loadWalletStatusJob?.isActive == true || isActiveAssistedWallet.not()) return
        loadWalletStatusJob = viewModelScope.launch {
            _uiState.update { state -> state.copy(isLoading = true) }
            getReplaceWalletStatusUseCase(
                GetReplaceWalletStatusUseCase.Param(args.groupId, args.walletId)
            ).onSuccess { status ->
                val verifiedSigners = status.signers.values
                    .filter { signer -> signer.verifyType != VerifyType.NONE }
                    .mapNotNull { signer -> signer.xfp }
                    .toSet()
                replacedSigners.apply {
                    clear()
                    addAll(status.signers.values)
                }
                _uiState.update { state ->
                    state.copy(
                        replaceSigners = status.signers.mapValues { entry ->
                            entry.value.toModel(
                                getIndexFromPathUseCase(entry.value.derivationPath.orEmpty())
                                    .getOrDefault(0)
                            )
                        },
                        coldCardBackUpFileName = status.signers.filter {
                            it.value.tags.contains(SignerTag.INHERITANCE.name) && it.value.type != SignerType.NFC
                        }.map { entry ->
                            entry.value.xfp.orEmpty() to entry.value.userBackUpFileName.orEmpty()
                        }.toMap(),
                        // Keyed by the slot being replaced, which is how every row here is keyed.
                        // The claim options and per-method verifications live on the replacement
                        // key the server holds, not on the key currently in the wallet.
                        inheritanceClaimStates = status.signers.mapValues { entry ->
                            entry.value.toInheritanceClaimState()
                        },
                        verifiedSigners = verifiedSigners,
                        pendingReplaceXfps = status.pendingReplaceXfps,
                    )
                }
                raisePendingClaimOptionsPrompt()
            }.onFailure {
                if (it !is CancellationException) {
                    _uiState.update { state -> state.copy(message = it.message.orUnknownError()) }
                }
            }
            _uiState.update { state -> state.copy(isLoading = false, isDataLoaded = true) }
        }
    }

    fun onCreateWallet() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            if (isActiveAssistedWallet) {
                finalizeReplaceAssistedWallet()
            } else {
                finalizeFreeWallet()
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun finalizeFreeWallet() {
        getWalletDetail2UseCase(args.walletId).onSuccess { oldWallet ->
            val newSigners = oldWallet.signers.map {
                savedStateHandle.get<SingleSigner>(it.masterFingerprint) ?: it
            }
            val newWallet = oldWallet.copy(signers = newSigners)
            createWallet2UseCase(
                newWallet
            ).onSuccess {
                updateWalletUseCase(
                    UpdateWalletUseCase.Params(
                        wallet = oldWallet.copy(name = "[DEPRECATED] ${oldWallet.name}"),
                        isAssistedWallet = false
                    )
                ).onFailure {
                    _uiState.update { state -> state.copy(message = it.message.orUnknownError()) }
                }
                _uiState.update { state -> state.copy(createWalletSuccess = StateEvent.String(it.id)) }
            }.onFailure {
                _uiState.update { state -> state.copy(message = it.message.orUnknownError()) }
            }
        }
    }

    private suspend fun finalizeReplaceAssistedWallet() {
        finalizeReplaceKeyUseCase(
            FinalizeReplaceKeyUseCase.Param(
                groupId = args.groupId,
                walletId = args.walletId
            )
        ).onSuccess { result ->
            if (args.groupId.isEmpty()) {
                syncPersonalWallets(Unit)
            } else {
                syncGroupWalletsUseCase(Unit)
            }
            _uiState.update { it.copy(createWalletSuccess = StateEvent.String(result.wallet.id)) }
        }.onFailure {
            _uiState.update { state -> state.copy(message = it.message.orUnknownError()) }
        }
    }

    private fun currentUserRole(members: List<ByzantineMember>): AssistedWalletRole {
        return members.firstOrNull {
            it.emailOrUsername == accountManager.getAccount().email
                    || it.emailOrUsername == accountManager.getAccount().username
        }?.role.toRole
    }

    fun setReplacingXfp(xfp: String) {
        viewModelScope.launch {
            savedStateHandle[REPLACE_XFP] = xfp
        }
    }

    fun initReplaceKey() {
        viewModelScope.launch {
            initReplaceKeyUseCase(
                InitReplaceKeyUseCase.Param(
                    groupId = args.groupId,
                    walletId = args.walletId,
                    xfp = savedStateHandle.get<String>(REPLACE_XFP).orEmpty()
                )
            )
        }
    }


    fun getTapSigners() =
        _uiState.value.signers.filter { it.type == SignerType.NFC && isSignerExist(it.fingerPrint).not() }

    fun getAllSigners() = _uiState.value.signers.filter {
        isSignerExist(it.fingerPrint).not()
    }.filter {
        (it.type == SignerType.COLDCARD_NFC && it.derivationPath.isRecommendedMultiSigPath)
                || it.type != SignerType.SERVER
    }

    fun getColdcard() = _uiState.value.signers.filter {
        isSignerExist(it.fingerPrint).not()
                && ((it.type == SignerType.COLDCARD_NFC && it.derivationPath.isRecommendedMultiSigPath)
                || (it.type == SignerType.AIRGAP && (it.tags.isEmpty() || it.tags.contains(SignerTag.COLDCARD))))
    }

    fun getAirgap(tag: SignerTag?): List<SignerModel> {
        return if (tag == null) {
            _uiState.value.signers.filter {
                it.type == SignerType.AIRGAP
                        && isSignerExist(it.fingerPrint).not()
            }
        } else {
            _uiState.value.signers.filter {
                isSignerExist(it.fingerPrint).not()
                        && (it.type == SignerType.AIRGAP
                        && (it.tags.contains(tag) || it.tags.isEmpty()))
            }
        }
    }

    fun getHardwareSigners(tag: SignerTag) =
        _uiState.value.signers.filter {
            it.type == SignerType.HARDWARE && it.tags.contains(tag) && isSignerExist(
                it.fingerPrint
            ).not()
        }

    fun getSoftwareSigners() =
        _uiState.value.signers.filter {
            (it.type == SignerType.SOFTWARE || it.type == SignerType.FOREIGN_SOFTWARE) && isSignerExist(
                it.fingerPrint
            ).not()
        }

    fun markOnCreateWalletSuccess() {
        _uiState.update { it.copy(createWalletSuccess = StateEvent.None) }
    }

    private fun isSignerExist(masterSignerId: String): Boolean {
        return _uiState.value.replaceSigners.containsKey(masterSignerId)
                || _uiState.value.walletSigners.any { it.fingerPrint == masterSignerId }
    }

    fun getCountWalletSoftwareSignersInDevice(): Int {
        val replaceSigners = _uiState.value.replaceSigners
        return _uiState.value.walletSigners.count {
            val signer = replaceSigners[it.fingerPrint] ?: it
            (signer.type == SignerType.SOFTWARE && signer.isVisible)
        }
    }

    fun onUpdateSignerTag(signer: SignerModel, tag: SignerTag) {
        viewModelScope.launch {
            singleSigners.find { it.masterFingerprint == signer.fingerPrint && it.derivationPath == signer.derivationPath }
                ?.let { singleSigner ->
                    updateRemoteSignerUseCase(singleSigner.copy(tags = listOf(tag)))
                        .onSuccess {
                            loadSigners()
                            onReplaceKey(singleSigner.copy(tags = listOf(tag)))
                        }.onFailure {
                            _uiState.update { state -> state.copy(message = it.message.orUnknownError()) }
                        }
                }
        }
    }

    fun onReplaceKey(signer: SingleSigner) {
        viewModelScope.launch {
            if (isActiveAssistedWallet) {
                _uiState.update { it.copy(isLoading = true) }
                replaceKeyUseCase(
                    ReplaceKeyUseCase.Param(
                        groupId = args.groupId,
                        walletId = args.walletId,
                        xfp = savedStateHandle.get<String>(REPLACE_XFP).orEmpty(),
                        signer = signer
                    )
                ).onSuccess {
                    loadWalletStatusJob?.cancel()
                    getReplaceWalletStatus()
                }.onFailure {
                    _uiState.update { state -> state.copy(message = it.message.orUnknownError()) }
                }
                _uiState.update { it.copy(isLoading = false) }
            } else {
                replaceFreeWalletSigner(signer)
            }
        }
    }

    private suspend fun replaceFreeWalletSigner(
        signer: SingleSigner
    ) {
        val xfp = savedStateHandle.get<String>(REPLACE_XFP).orEmpty()
        savedStateHandle[xfp] = signer
        val signerModel = singleSignerMapper(signer)
        val signerMap = uiState.value.replaceSigners.toMutableMap().apply {
            this[xfp] = signerModel
        }
        _uiState.update { state ->
            state.copy(replaceSigners = signerMap)
        }
    }

    fun onHandledMessage() {
        _uiState.update { it.copy(message = "") }
    }

    fun onCancelReplaceWallet() {
        viewModelScope.launch {
            if (isActiveAssistedWallet) {
                resetReplaceKeyUseCase(
                    ResetReplaceKeyUseCase.Param(
                        groupId = args.groupId,
                        walletId = args.walletId
                    )
                ).onSuccess {
                    getReplaceWalletStatus()
                }.onFailure {
                    _uiState.update { state -> state.copy(message = it.message.orUnknownError()) }
                }
            } else {
                val replaceSigners = _uiState.value.replaceSigners.values
                replaceSigners.forEach {
                    savedStateHandle.remove<SingleSigner>(it.fingerPrint)
                }
                _uiState.update { state -> state.copy(replaceSigners = emptyMap()) }
            }
        }
    }

    fun getKeyId(xfp: String): String {
        return replacedSigners.find { it.xfp == xfp }?.userKeyId.orEmpty()
    }

    fun getFilePath(xfp: String) = nfcFileManager.buildFilePath(getKeyId(xfp))

    fun getBackUpFileName(xfp: String) = _uiState.value.coldCardBackUpFileName[xfp].orEmpty()

    val replacedXfp: String
        get() = savedStateHandle.get<String>(REPLACE_XFP).orEmpty()

    fun isMultiSig() = _uiState.value.isMultiSig
    fun getPortalSigners() =
        _uiState.value.signers.filter { it.type == SignerType.PORTAL_NFC && isSignerExist(it.fingerPrint).not() }

    fun getReplaceSignerXfp(xfp: String): String {
        val replaceSigners = _uiState.value.replaceSigners
        return replaceSigners.entries.find { it.value.fingerPrint == xfp }?.key.orEmpty()
    }

    fun isEnableContinueButton(): Boolean {
        val state = _uiState.value
        if (state.replaceSigners.isEmpty()) return false
        // A replacement inheritance key with no sharing method recorded, or one whose artifacts
        // nobody has dealt with, cannot go into the wallet: the Beneficiary would have no way to
        // reach it. Skipping a verification settles it, so this never traps the owner.
        return state.inheritanceIncompleteSlots.isEmpty()
    }

    fun onRemoveKey(xfp: String) {
        if (!isActiveAssistedWallet) {
            val replaceSigners = _uiState.value.replaceSigners.toMutableMap().apply {
                remove(xfp)
            }
            _uiState.update { state -> state.copy(replaceSigners = replaceSigners) }
        } else {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                removeKeyReplacementUseCase(
                    RemoveKeyReplacementUseCase.Params(
                        groupId = args.groupId,
                        walletId = args.walletId,
                        xfp = xfp
                    )
                ).onSuccess {
                    val replaceSigners = _uiState.value.replaceSigners.toMutableMap().apply {
                        remove(xfp)
                    }
                    _uiState.update { state -> state.copy(replaceSigners = replaceSigners) }
                }.onFailure {
                    _uiState.update { state -> state.copy(message = it.message.orUnknownError()) }
                }
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * The keys the wallet already holds, so the inheritance picker does not offer one of them for
     * the slot being replaced. It covers the pending replacements too — a key can only go into the
     * wallet once.
     */
    fun existingWalletSigners(): List<SignerModel> =
        _uiState.value.walletSigners + _uiState.value.replaceSigners.values

    /**
     * The off-chain inheritance key of [slotXfp] as the server records it: how the owner chose to
     * pass it on, and how far each of those artifacts has got. Empty for every other slot, and
     * while the replacement status has not landed.
     */
    fun inheritanceClaimState(slotXfp: String): InheritanceClaimState =
        _uiState.value.inheritanceClaimStates[slotXfp] ?: InheritanceClaimState()

    /**
     * Called when the inheritance picker has put a key on the slot, so the sharing-method choice
     * opens as soon as the replacement status confirms it.
     *
     * It is a flag rather than an immediate emission because the key is saved while the add-key
     * screen is still on top: the status that carries it lands afterwards, and this screen is
     * stopped in the meantime.
     */
    fun onInheritanceKeyAdded() {
        val slot = savedStateHandle.get<String>(REPLACE_XFP).orEmpty()
        if (slot.isEmpty()) return
        promptClaimOptionsForSlot = slot
        promptClaimOptionsPreviousXfp = _uiState.value.replaceSigners[slot]?.fingerPrint
    }

    fun onClaimOptionsPromptHandled() {
        _uiState.update { it.copy(pendingClaimOptionsSigner = null) }
    }

    private fun raisePendingClaimOptionsPrompt() {
        val slot = promptClaimOptionsForSlot ?: return
        val signer = _uiState.value.replaceSigners[slot] ?: return
        // Backing out of the picker leaves the slot as it was; only a key that actually landed
        // owes a sharing method.
        if (signer.fingerPrint == promptClaimOptionsPreviousXfp) return
        promptClaimOptionsForSlot = null
        promptClaimOptionsPreviousXfp = null
        _uiState.update { it.copy(pendingClaimOptionsSigner = signer) }
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
        _uiState.update { it.copy(requestTapSignerCardTap = false) }
    }

    private suspend fun handleTapSignerOutcome(outcome: TapSignerKeyResolver.Outcome) {
        when (outcome) {
            is TapSignerKeyResolver.Outcome.Resolved -> onReplaceKey(outcome.signer)
            TapSignerKeyResolver.Outcome.NeedsCardTap ->
                _uiState.update { it.copy(requestTapSignerCardTap = true) }

            is TapSignerKeyResolver.Outcome.Failed ->
                _uiState.update { it.copy(message = outcome.message) }
        }
    }

    /**
     * Records that the seed-phrase backup of a replacement inheritance key was proven: the owner
     * restored it onto a device and the device reported the fingerprint back.
     *
     * Only the keys that pair in-app (Ledger, BitBox) come back here — Coldcard and air-gap mark
     * themselves verified inside their own add-key screens and never return a fingerprint.
     */
    fun onSeedPhraseBackupVerified(masterSignerId: String) {
        viewModelScope.launch {
            setReplaceKeyVerifiedUseCase(
                SetReplaceKeyVerifiedUseCase.Param(
                    keyId = masterSignerId,
                    checkSum = "",
                    verifyType = VerifyType.APP_VERIFIED,
                    groupId = args.groupId,
                    walletId = args.walletId,
                    verificationMethod = ClaimOption.SEED_PHRASE,
                )
            ).onSuccess {
                getReplaceWalletStatus()
                _uiState.update { it.copy(seedPhraseVerifiedSigner = StateEvent.String(masterSignerId)) }
            }.onFailure {
                _uiState.update { state -> state.copy(message = it.message.orUnknownError()) }
            }
        }
    }

    fun onSeedPhraseVerifiedHandled() {
        _uiState.update { it.copy(seedPhraseVerifiedSigner = StateEvent.None) }
    }

    companion object {
        const val REPLACE_XFP = "REPLACE_XFP"
    }
}


data class ReplaceKeysUiState(
    val isDataLoaded: Boolean = false,
    val pendingReplaceXfps: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val walletSigners: List<SignerModel> = emptyList(),
    val replaceSigners: Map<String, SignerModel> = emptyMap(),
    val verifiedSigners: Set<String> = emptySet(),
    val group: ByzantineGroup? = null,
    val myRole: AssistedWalletRole = AssistedWalletRole.NONE,
    val createWalletSuccess: StateEvent = StateEvent.None,
    val signers: List<SignerModel> = emptyList(),
    val message: String = "",
    val inheritanceXfps: Set<String> = emptySet(),
    val isActiveAssistedWallet: Boolean = false,
    val isMultiSig: Boolean = false,
    val coldCardBackUpFileName: Map<String, String> = emptyMap(),
    /**
     * Claim state of the replacement key on each slot, keyed by the slot being replaced. Only an
     * off-chain inheritance slot carries anything; everything else stays empty.
     */
    val inheritanceClaimStates: Map<String, InheritanceClaimState> = emptyMap(),
    /** Replacement inheritance key waiting for the owner to pick how it reaches the Beneficiary. */
    val pendingClaimOptionsSigner: SignerModel? = null,
    /** The TAPSIGNER picked for the inheritance slot needs another tap to read its xpub. */
    val requestTapSignerCardTap: Boolean = false,
    /** The seed-phrase backup of this key was just proven; the confirmation screen follows. */
    val seedPhraseVerifiedSigner: StateEvent = StateEvent.None,
    val addressType: AddressType = AddressType.NATIVE_SEGWIT
) {
    /**
     * Whether [slotXfp] holds the wallet's inheritance key.
     *
     * [inheritanceXfps] is the server's answer but arrives from a network call of its own. The
     * local wallet carries the same tag and is there immediately — it is what `replaceKey` itself
     * reads to decide whether the replacement is an inheritance key — so neither the row nor the
     * Continue gate hangs on that call, or silently opens up if it never lands.
     */
    fun isInheritanceSlot(slotXfp: String): Boolean = isActiveAssistedWallet &&
            (inheritanceXfps.contains(slotXfp) || walletSigners.any {
                it.fingerPrint == slotXfp && it.tags.contains(SignerTag.INHERITANCE)
            })

    /**
     * Inheritance slots whose replacement key still owes a sharing method or a verification. Only
     * an assisted wallet tracks any of this; a free wallet has no inheritance key.
     */
    val inheritanceIncompleteSlots: Set<String>
        get() = replaceSigners.keys.filter { slot ->
            isInheritanceSlot(slot) && inheritanceClaimStates[slot]?.isIncomplete == true
        }.toSet()
}