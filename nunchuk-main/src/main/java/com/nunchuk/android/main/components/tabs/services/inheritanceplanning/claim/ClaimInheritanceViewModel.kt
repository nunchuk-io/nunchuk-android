package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim

import com.nunchuk.android.core.util.UiText
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.base.MutableSaveStateFlow
import com.nunchuk.android.core.base.update
import com.nunchuk.android.core.domain.ParseWalletDescriptorUseCase
import com.nunchuk.android.core.domain.membership.DownloadWalletForClaimUseCase
import com.nunchuk.android.core.domain.membership.GetClaimSigningChallengeUseCase
import com.nunchuk.android.core.domain.membership.GetClaimingWalletUseCase
import com.nunchuk.android.core.domain.membership.GetInheritanceClaimStateUseCase
import com.nunchuk.android.core.mapper.SingleSignerMapper
import com.nunchuk.android.core.network.ApiErrorCode.INHERITANCE_PLAN_NOT_ACTIVE
import com.nunchuk.android.core.network.ApiErrorCode.INHERITANCE_PLAN_NOT_FOUND
import com.nunchuk.android.core.network.NunchukApiException
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toSingleSigner
import com.nunchuk.android.core.util.multiSigAccountIndex
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover.InheritanceOption
import com.nunchuk.android.model.BufferPeriodCountdown
import com.nunchuk.android.model.InheritanceAdditional
import com.nunchuk.android.model.InheritanceClaimingInit
import com.nunchuk.android.model.KeyOrigin
import com.nunchuk.android.model.inheritance.ClaimDownloadWalletResult
import com.nunchuk.android.model.inheritance.ClaimValidKey
import com.nunchuk.android.model.inheritance.ClaimSigningChallenge
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.CreateSoftwareSignerUseCase
import com.nunchuk.android.usecase.DeleteMasterSignerUseCase
import com.nunchuk.android.usecase.GetMasterFingerprintUseCase
import com.nunchuk.android.usecase.membership.DeletePendingRequestsByMagicUseCase
import com.nunchuk.android.usecase.membership.GetAddedKeysForInheritanceUseCase
import com.nunchuk.android.usecase.signer.GetDefaultSignerFromMasterSignerUseCase
import com.nunchuk.android.usecase.signer.GetSignerFromMasterSignerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import javax.inject.Inject
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.verifymessage.ImportSignatureVia
import com.nunchuk.android.main.R

@HiltViewModel
class ClaimInheritanceViewModel @Inject constructor(
    private val createSoftwareSignerUseCase: CreateSoftwareSignerUseCase,
    private val getDefaultSignerFromMasterSignerUseCase: GetDefaultSignerFromMasterSignerUseCase,
    private val downloadWalletForClaimUseCase: DownloadWalletForClaimUseCase,
    private val getInheritanceClaimStateUseCase: GetInheritanceClaimStateUseCase,
    private val getMasterFingerprintUseCase: GetMasterFingerprintUseCase,
    private val deleteMasterSignerUseCase: DeleteMasterSignerUseCase,
    private val singleSignerMapper: SingleSignerMapper,
    private val getClaimingWalletUseCase: GetClaimingWalletUseCase,
    private val parseWalletDescriptorUseCase: ParseWalletDescriptorUseCase,
    private val getAddedKeysForInheritanceUseCase: GetAddedKeysForInheritanceUseCase,
    private val deletePendingRequestsByMagicUseCase: DeletePendingRequestsByMagicUseCase,
    private val getClaimSigningChallengeUseCase: GetClaimSigningChallengeUseCase,
    private val getSignerFromMasterSignerUseCase: GetSignerFromMasterSignerUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val handledRequestIds = mutableSetOf<String>()

    // Software-key accounts derived and sent to download-wallet. One asked for again was rejected,
    // so it is not derived twice (no loop); a failed derivation is not recorded and can be retried.
    private val submittedDerivations = mutableSetOf<KeyOrigin>()

    private val _claimData = MutableSaveStateFlow(
        savedStateHandle = savedStateHandle,
        key = KEY_CLAIM_DATA,
        defaultValue = ClaimData()
    )
    val claimData: StateFlow<ClaimData> = _claimData.asStateFlow()

    private val _uiState = MutableStateFlow(ClaimUiState())
    val uiState: StateFlow<ClaimUiState> = _uiState

    fun reset() {
        _claimData.update { ClaimData() }
        _uiState.update { ClaimUiState() }
        handledRequestIds.clear()
        submittedDerivations.clear()
    }

    fun setClaimDataByBackup(
        signers: List<SignerModel>,
        magic: String,
        inheritanceAdditional: InheritanceAdditional,
    ) {
        deletePendingRequests(magic)
        submittedDerivations.clear()
        _claimData.update {
            it.copy(
                signers = signers.toSet(),
                magic = magic,
                inheritanceAdditional = inheritanceAdditional,
                challenge = null
            )
        }
    }

    fun updateClaimInit(magicPhrase: String, init: InheritanceClaimingInit) {
        deletePendingRequests(magicPhrase)
        submittedDerivations.clear()
        _claimData.update {
            it.copy(
                signers = emptySet(),
                signatures = emptyList(),
                magic = magicPhrase,
                // With several counts CLM-KC picks one; the smallest stands until then.
                requiredKeyCount = init.requiredKeyCounts.firstOrNull() ?: init.inheritanceKeyCount,
                requiredKeyCounts = init.requiredKeyCounts,
                pendingKeyOrigins = emptyList(),
                walletType = init.walletType,
                keyOrigins = init.keyOrigins,
                showSeedPhraseGuidance = init.showSeedPhraseGuidance,
                challenge = null
            )
        }
    }

    /** CLM-KC: the number of independent keys the heir received sets the collection target. */
    fun selectRequiredKeyCount(count: Int) {
        _claimData.update { it.copy(requiredKeyCount = count) }
    }

    fun deletePendingRequests(magic: String) {
        viewModelScope.launch {
            if (magic.isNotEmpty()) {
                deletePendingRequestsByMagicUseCase(
                    DeletePendingRequestsByMagicUseCase.Param(
                        magic,
                    )
                )
            }
        }
    }

    fun createSoftwareSignerFromMnemonic(mnemonic: String, passphrase: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val currentData = _claimData.value
            val signerName = "$INHERITED_KEY_NAME #${currentData.signers.size + 1}"

            runCatching {
                getMasterFingerprintUseCase(
                    GetMasterFingerprintUseCase.Param(
                        mnemonic = mnemonic,
                        passphrase = passphrase
                    )
                ).getOrThrow()?.let {
                    deleteMasterSignerUseCase(it).getOrThrow()
                }
            }

            createSoftwareSignerUseCase(
                CreateSoftwareSignerUseCase.Param(
                    name = signerName,
                    mnemonic = mnemonic,
                )
            ).map { signer ->
                getDefaultSignerFromMasterSignerUseCase(
                    GetDefaultSignerFromMasterSignerUseCase.Params(
                        masterSignerId = signer.id,
                        walletType = WalletType.MULTI_SIG,
                        addressType = AddressType.NATIVE_SEGWIT
                    )
                ).onFailure { e ->
                    Timber.e(e)
                    _uiState.update {
                        it.copy(
                            event = ClaimInheritanceEvent.ShowError(e.message.orUnknownError()),
                            isLoading = false
                        )
                    }
                }.getOrNull()
            }.onSuccess { signer ->
                signer?.let {
                    addSigner(singleSignerMapper(signer))
                }
            }.onFailure { e ->
                Timber.e(e)
                _uiState.update {
                    it.copy(
                        event = ClaimInheritanceEvent.ShowError(e.message.orUnknownError()),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun addSigner(signer: SignerModel) {
        val data = _claimData.value
        if (!data.isOnChainClaim) {
            addOffChainSigner(data, signer)
            return
        }
        when (val step = data.addOnChainSigner(signer)) {
            is OnChainKeyStep.WrongAccount -> showAddAccount(step.origin)
            OnChainKeyStep.NotInPlan -> showIncorrectKey()
            OnChainKeyStep.AlreadyAdded -> _uiState.update {
                it.copy(event = ClaimInheritanceEvent.KeyAlreadyAdded, isLoading = false)
            }
            is OnChainKeyStep.Download -> {
                _claimData.update { step.data }
                _uiState.update { it.copy(event = ClaimInheritanceEvent.SignerAdded) }
                viewModelScope.launch { downloadWalletForClaim() }
            }
            is OnChainKeyStep.AddMore -> {
                _claimData.update { step.data }
                _uiState.update {
                    it.copy(
                        event = ClaimInheritanceEvent.AddMoreSigners,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun addOffChainSigner(data: ClaimData, signer: SignerModel) {
        when {
            !data.isInPlanOrigins(signer) -> showIncorrectKey()
            data.signers.any { it.isSameAccount(signer) } -> _uiState.update {
                it.copy(event = ClaimInheritanceEvent.KeyAlreadyAdded, isLoading = false)
            }
            else -> {
                _claimData.update { it.copy(signers = it.signers + signer) }
                _uiState.update {
                    it.copy(
                        event = ClaimInheritanceEvent.SignMessage(signer),
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun showIncorrectKey() {
        _uiState.update {
            it.copy(
                event = ClaimInheritanceEvent.ShowError(UiText.resource(R.string.nc_main_incorrect_key_add_correct_key)),
                isLoading = false
            )
        }
    }

    private suspend fun downloadWalletForClaim() {
        val currentData = _claimData.value
        downloadWalletForClaimUseCase(
            DownloadWalletForClaimUseCase.Param(
                magic = currentData.magic,
                keys = currentData.signers.map { it.toSingleSigner() },
            )
        ).map { result ->
            when (result) {
                is ClaimDownloadWalletResult.Downloaded -> {
                    val wallet = result.wallet
                    _claimData.update { it.copy(pendingKeyOrigins = emptyList()) }
                    _uiState.update {
                        it.copy(
                            isRequiredRegister = wallet.requiresRegistration,
                            walletId = wallet.localId
                        )
                    }
                    getInheritanceStatus(currentData.magic, wallet.bsms)
                }

                is ClaimDownloadWalletResult.BufferPeriod -> _uiState.update {
                    it.copy(
                        event = ClaimInheritanceEvent.NavigateToBufferPeriod(result.countdown),
                        isLoading = false
                    )
                }

                is ClaimDownloadWalletResult.MoreKeysNeeded -> onMoreKeysNeeded(result)
            }
        }.onFailure { e ->
            if (e is NunchukApiException && (e.code == INHERITANCE_PLAN_NOT_FOUND || e.code == INHERITANCE_PLAN_NOT_ACTIVE)) {
                _uiState.update {
                    it.copy(
                        event = ClaimInheritanceEvent.NavigateToInheritanceError(
                            errorCode = e.code,
                            message = e.message
                        ),
                        isLoading = false
                    )
                }
            } else if (e !is NunchukApiException) {
                // Network or download failure: the accepted keys stay, so the same try can run again.
                Timber.e(e)
                _uiState.update {
                    it.copy(
                        retryDownloadMessage = e.message.orUnknownError(),
                        retryAction = RetryAction.DOWNLOAD,
                        isLoading = false,
                    )
                }
            } else {
                Timber.e(e)
                _uiState.update {
                    it.copy(
                        event = ClaimInheritanceEvent.ShowError(e.message.orUnknownError()),
                        isLoading = false
                    )
                }
            }
        }
    }

    /** Retry after a network failure: download-wallet again, or the account derivation that failed. */
    fun retryDownload() {
        val retry = _uiState.value.retryAction
        _uiState.update { it.copy(retryDownloadMessage = null, isLoading = true) }
        viewModelScope.launch {
            when (retry) {
                RetryAction.DOWNLOAD -> downloadWalletForClaim()
                RetryAction.IMPORT_ACCOUNTS -> importPendingAccounts()
            }
        }
    }

    fun onRetryDownloadDismissed() {
        _uiState.update { it.copy(retryDownloadMessage = null) }
    }

    /**
     * No condition is complete yet. Keeps the accepted accounts, then imports the accounts asked
     * for (`key_origins`), or asks for another key when none are.
     */
    private suspend fun onMoreKeysNeeded(result: ClaimDownloadWalletResult.MoreKeysNeeded) {
        val before = _claimData.value
        val after = before.withDownloadResult(result.validKeys, result.keyOrigins)
        _claimData.update { after }
        if (after.pendingKeyOrigins.isNotEmpty()) {
            importPendingAccounts()
        } else {
            _uiState.update {
                it.copy(
                    event = ClaimInheritanceEvent.AddAnotherKey(rejectedKey = after.signers.size < before.signers.size),
                    isLoading = false
                )
            }
        }
    }

    /**
     * Imports the next requested account. A software key's is derived here and submitted at once,
     * like any imported account; a device's needs the heir (CLM-16).
     */
    private suspend fun importPendingAccounts() {
        when (val step = _claimData.value.nextAccountImport(submittedDerivations)) {
            is AccountImportStep.Derive -> deriveAccount(step.origin)
                .onSuccess { account ->
                    // Recorded only once it is sent: a derivation that failed can be retried.
                    submittedDerivations.add(step.origin)
                    _claimData.update {
                        it.copy(signers = it.signers + account, pendingKeyOrigins = it.pendingKeyOrigins - step.origin)
                    }
                    downloadWalletForClaim()
                }
                .onFailure { e ->
                    Timber.e(e)
                    _uiState.update {
                        it.copy(
                            retryDownloadMessage = e.message.orUnknownError(),
                            retryAction = RetryAction.IMPORT_ACCOUNTS,
                            isLoading = false,
                        )
                    }
                }

            is AccountImportStep.AskDevice -> _uiState.update {
                it.copy(addAccountPrompt = AddAccountPrompt(account = step.account, device = step.device), isLoading = false)
            }

            AccountImportStep.AddAnotherKey -> _uiState.update {
                it.copy(event = ClaimInheritanceEvent.AddAnotherKey(rejectedKey = true), isLoading = false)
            }
        }
    }

    private suspend fun deriveAccount(origin: KeyOrigin): Result<SignerModel> = runCatching {
        val account = requireNotNull(origin.derivationPath.multiSigAccountIndex) { "No account in ${origin.derivationPath}" }
        val signer = getSignerFromMasterSignerUseCase(
            GetSignerFromMasterSignerUseCase.Param(
                xfp = origin.xfp,
                walletType = WalletType.MULTI_SIG,
                addressType = AddressType.NATIVE_SEGWIT,
                index = account,
            )
        ).getOrThrow()
        requireNotNull(signer?.let { singleSignerMapper(it) }?.takeIf { origin.matches(it) }) {
            "Could not derive account $account of ${origin.xfp}"
        }
    }

    /** CLM-16 for a device that came back with the wrong account: ask for [origin] again. */
    private fun showAddAccount(origin: KeyOrigin) {
        val device = _claimData.value.deviceOf(origin)
        val account = origin.derivationPath.multiSigAccountIndex
        _uiState.update {
            if (device == null || account == null) {
                it.copy(event = ClaimInheritanceEvent.AddAnotherKey(rejectedKey = true), isLoading = false)
            } else {
                it.copy(addAccountPrompt = AddAccountPrompt(account = account, device = device), isLoading = false)
            }
        }
    }

    fun onAddAccountPromptHandled() {
        _uiState.update { it.copy(addAccountPrompt = null) }
    }

    fun getInheritanceStatus(magic: String = "", bsms: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getInheritanceClaimStateUseCase(
                GetInheritanceClaimStateUseCase.Param(
                    magic = magic,
                    bsms = bsms
                )
            ).onFailure { e ->
                Timber.e(e)
                _uiState.update {
                    it.copy(
                        event = ClaimInheritanceEvent.ShowError(e.message.orUnknownError()),
                    )
                }
            }.onSuccess { inheritanceAdditional ->
                inheritanceAdditional.let {
                    _claimData.update {
                        it.copy(
                            inheritanceAdditional = inheritanceAdditional,
                            bsms = bsms
                        )
                    }
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun getClaimingWallet(bsms: String) = viewModelScope.launch {
        parseWalletDescriptorUseCase(bsms).onSuccess { localWallet ->
            getClaimingWalletUseCase(localWallet.id).onSuccess { wallet ->
                _uiState.update {
                    it.copy(
                        walletId = wallet.localId,
                        isRequiredRegister = wallet.requiresRegistration,
                    )
                }
                _claimData.update {
                    it.copy(
                        walletType = wallet.walletType,
                    )
                }
            }
        }
    }

    /**
     * "Import signature" pressed on the export-completed screen; the verify screen, which owns the
     * launchers, performs the import over [via].
     */
    fun requestImportSignature(via: ImportSignatureVia) {
        _uiState.update {
            it.copy(
                event = ClaimInheritanceEvent.ImportSignature(via),
            )
        }
    }

    fun onEventHandled() {
        _uiState.update { it.copy(event = null) }
    }

    fun checkRequestedAddDesktopKey() {
        viewModelScope.launch {
            getAddedKeysForInheritanceUseCase(
                GetAddedKeysForInheritanceUseCase.Param(
                    magic = claimData.value.magic
                )
            ).onSuccess { addedKeys ->
                addedKeys.forEach { entry ->
                    if (!handledRequestIds.contains(entry.key)) {
                        val signerModel = singleSignerMapper(entry.value)
                        addSigner(signerModel)
                    }
                }
                handledRequestIds.addAll(addedKeys.map { it.key })
            }.onFailure { e ->
                Timber.e(e)
                _uiState.update {
                    it.copy(
                        event = ClaimInheritanceEvent.ShowError(e.message.orUnknownError()),
                    )
                }
            }
        }
    }

    fun generateClaimSigningChallengeIfNeeded(option: InheritanceOption) {
        viewModelScope.launch {
            val currentData = _claimData.value
            if (currentData.challenge == null && currentData.magic.isNotEmpty() && !currentData.isOnChainClaim) {
                getClaimSigningChallengeUseCase(currentData.magic)
                    .onSuccess { challenge ->
                        _uiState.update {
                            it.copy(
                                event = ClaimInheritanceEvent.GenerateChallengeSuccess(option),
                            )
                        }
                        _claimData.update { it.copy(challenge = challenge) }
                    }
                    .onFailure { e ->
                        Timber.e(e)
                        _uiState.update {
                            it.copy(
                                event = ClaimInheritanceEvent.ShowError(e.message.orUnknownError()),
                            )
                        }
                    }
            } else {
                _uiState.update {
                    it.copy(
                        event = ClaimInheritanceEvent.GenerateChallengeSuccess(option),
                    )
                }
            }
        }
    }

    fun updateInheritanceAdditional(inheritanceAdditional: InheritanceAdditional) {
        _claimData.update { it.copy(inheritanceAdditional = inheritanceAdditional) }
    }

    fun addSignature(signature: String) {
        val currentData = _claimData.value
        _claimData.update {
            it.copy(
                signatures = currentData.signatures + signature
            )
        }
    }

    private companion object {
        private const val KEY_CLAIM_DATA = "claim_data"
        private const val INHERITED_KEY_NAME = "Inheritance key"
    }
}

data class ClaimUiState(
    val event: ClaimInheritanceEvent? = null,
    val isLoading: Boolean = false,
    val walletId: String = "",
    val isRequiredRegister: Boolean = false,
    val addAccountPrompt: AddAccountPrompt? = null,
    // download-wallet failed outside the API (network); shown with Retry.
    val retryDownloadMessage: String? = null,
    val retryAction: RetryAction = RetryAction.DOWNLOAD,
)

/** What Retry runs again after a failure outside the API. */
enum class RetryAction { DOWNLOAD, IMPORT_ACCOUNTS }

/** CLM-16: add [account] from [device], which the heir already added at another account. */
data class AddAccountPrompt(val account: Int, val device: SignerModel)

sealed class ClaimInheritanceEvent {
    data class ShowError(val message: UiText) : ClaimInheritanceEvent() {
        constructor(message: String) : this(UiText.Dynamic(message))
    }
    data class NavigateToInheritanceError(
        val errorCode: Int,
        val message: String
    ) : ClaimInheritanceEvent()

    data object AddMoreSigners : ClaimInheritanceEvent()

    /** download-wallet found no complete condition and nothing to import; [rejectedKey] when it dropped a key. */
    data class AddAnotherKey(val rejectedKey: Boolean) : ClaimInheritanceEvent()
    data class NavigateToBufferPeriod(val countdown: BufferPeriodCountdown) : ClaimInheritanceEvent()
    data object KeyAlreadyAdded : ClaimInheritanceEvent()
    data object SignerAdded : ClaimInheritanceEvent()
    data class SignMessage(val signer: SignerModel) : ClaimInheritanceEvent()
    data class GenerateChallengeSuccess(val option: InheritanceOption) : ClaimInheritanceEvent()
    data class ImportSignature(val via: ImportSignatureVia) : ClaimInheritanceEvent()
}

@Parcelize
data class ClaimData(
    val signers: Set<SignerModel> = emptySet(),
    val signatures: List<String> = emptyList(),
    val magic: String = "",
    val inheritanceAdditional: InheritanceAdditional? = null,
    val requiredKeyCount: Int = 1,
    val walletType: WalletType = WalletType.MULTI_SIG,
    val keyOrigins: List<KeyOrigin> = emptyList(),
    val bsms: String? = null,
    val challenge: ClaimSigningChallenge? = null,
    /** `required_key_counts` from claim init; empty from a backend that only sends one count. */
    val requiredKeyCounts: List<Int> = emptyList(),
    /** Further accounts of accepted devices that download-wallet asked for (`key_origins`). */
    val pendingKeyOrigins: List<KeyOrigin> = emptyList(),
    /** `show_seed_phrase_guidance` from claim init; null from a backend that does not send it. */
    val showSeedPhraseGuidance: Boolean? = null,
) : Parcelable {
    /**
     * The backend counts independent keys: several accounts of one device (one XFP) are one key.
     * Without `required_key_counts` every added signer counts, as before.
     */
    val isIndependentKeyCount: Boolean
        get() = requiredKeyCounts.isNotEmpty()

    /** Keys added towards [requiredKeyCount]. */
    val addedKeyCount: Int
        get() = if (isIndependentKeyCount) signers.distinctBy { it.fingerPrint.lowercase() }.size else signers.size

    /** CLM-KC is shown only when the plan's conditions need different numbers of keys. */
    val needsKeyCountChoice: Boolean
        get() = requiredKeyCounts.size > 1

    /**
     * Signers for the claim transaction. With independent keys one account per device goes in: the
     * last one added, which is the account download-wallet asked for after account 0.
     */
    val requiredSigners: List<SignerModel>
        get() = signers.takeIf { bsms.isNullOrEmpty() }.orEmpty().toList().let { list ->
            if (isIndependentKeyCount) list.asReversed().distinctBy { it.fingerPrint.lowercase() }.asReversed() else list
        }

    /** The pending account [signer] is, or null when it is not one download-wallet asked for. */
    fun pendingOriginOf(signer: SignerModel): KeyOrigin? =
        pendingKeyOrigins.firstOrNull { it.matches(signer) }

    /** The added device (latest account) [origin] belongs to. */
    fun deviceOf(origin: KeyOrigin): SignerModel? =
        signers.lastOrNull { it.fingerPrint.equals(origin.xfp, ignoreCase = true) }

    /** Whether [signer] is a device with a pending account, whichever account it came with. */
    fun isPendingDevice(signer: SignerModel): Boolean =
        pendingKeyOrigins.any { it.xfp.equals(signer.fingerPrint, ignoreCase = true) }

    /**
     * Keeps only the signers download-wallet accepted (full XFP / path / xpub, the xpub when both
     * sides know it) and records the accounts it still needs, minus any already added.
     */
    fun withDownloadResult(validKeys: List<ClaimValidKey>, origins: List<KeyOrigin>): ClaimData {
        val accepted = signers.filterTo(LinkedHashSet()) { signer -> validKeys.any { it.matches(signer) } }
        return copy(
            signers = accepted,
            pendingKeyOrigins = origins.filter { origin -> accepted.none { origin.matches(it) } },
        )
    }

    val isOnChainClaim: Boolean
        get() = !bsms.isNullOrEmpty() || walletType == WalletType.MINISCRIPT

    /**
     * CLM-KCa/KCb: a claim that collects [keyCount] ≥ 2 keys first recommends separate devices when
     * the server sends `show_seed_phrase_guidance`. Without it (older backend) only on-chain claims
     * show it, as before.
     */
    fun needsSeparateDevicesNotice(keyCount: Int): Boolean =
        keyCount >= 2 && (showSeedPhraseGuidance ?: isOnChainClaim)

    /** On-chain CLM-KCa/KCb sits before the "requires N keys" intro (or the key-count chooser). */
    fun needsSeparateDevicesBeforeIntro(keyCount: Int): Boolean =
        isOnChainClaim && needsSeparateDevicesNotice(keyCount)

    /**
     * OFF-CLM-KCa sits after the "requires 2 keys" intro of the first key, before the claim method
     * choice (Backup Password check).
     */
    fun needsSeparateDevicesAfterIntro(keyIndex: Int): Boolean =
        !isOnChainClaim && keyIndex == 1 && needsSeparateDevicesNotice(requiredKeyCount)

    /**
     * One path per [requiredSigners] entry, in that order — the two lists travel together into
     * the claiming transaction and are read by index. The plan's key origins are the source; a
     * signer's own path stands in when the plan has none (legacy claims) or the origin cannot be
     * told apart (a master signer that holds several accounts).
     */
    val derivationPaths: List<String>
        get() = requiredSigners.map { signer -> signer.originDerivationPath() }

    private fun SignerModel.originDerivationPath(): String =
        keyOrigins.firstOrNull { it.xfp == fingerPrint && (isMasterSigner || it.derivationPath == derivationPath) }
            ?.derivationPath
            ?: keyOrigins.firstOrNull { it.xfp == fingerPrint }?.derivationPath
            ?: derivationPath

    /** Whether one of the added signers is this origin's key. */
    private fun KeyOrigin.isAdded(): Boolean =
        signers.any { it.fingerPrint == xfp && (it.isMasterSigner || it.derivationPath == derivationPath) }

    /**
     * Account the next inheritance key is expected at: taken from the first key of the plan not
     * added yet. -1 when the plan does not say (no key origins, or all keys already added). Two
     * keys of one device share an XFP, so an origin counts as added only when a signer sits at
     * its path too. Accounts download-wallet asks for ([pendingKeyOrigins]) never apply here: they
     * belong to one device and are imported from it (CLM-16); a new device starts at account 0.
     */
    val nextKeyAccountIndex: Int
        get() = keyOrigins.firstOrNull { origin -> !origin.isAdded() }
            ?.derivationPath?.multiSigAccountIndex ?: -1
}

internal fun String.normalizedPath(): String = trim().lowercase().replace('h', '\'')

internal fun KeyOrigin.matches(signer: SignerModel): Boolean =
    xfp.equals(signer.fingerPrint, ignoreCase = true) &&
        derivationPath.normalizedPath() == signer.derivationPath.normalizedPath()

private fun ClaimValidKey.matches(signer: SignerModel): Boolean =
    xfp.equals(signer.fingerPrint, ignoreCase = true) &&
        derivationPath.normalizedPath() == signer.derivationPath.normalizedPath() &&
        (xpub.isEmpty() || signer.xpub.isEmpty() || xpub == signer.xpub)

