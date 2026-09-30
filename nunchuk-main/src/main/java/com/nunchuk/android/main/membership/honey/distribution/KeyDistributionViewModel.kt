package com.nunchuk.android.main.membership.honey.distribution

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.SupportedSignerConfig
import com.nunchuk.android.model.inheritance.ClaimOption
import com.nunchuk.android.main.membership.model.InheritanceClaimState
import com.nunchuk.android.main.membership.model.toInheritanceClaimState
import com.nunchuk.android.usecase.membership.SetInheritanceClaimOptionsUseCase
import com.nunchuk.android.usecase.membership.SyncDraftWalletUseCase
import com.nunchuk.android.usecase.replace.GetReplaceWalletStatusUseCase
import com.nunchuk.android.usecase.GetUserWalletConfigsSetupFromCacheUseCase
import com.nunchuk.android.usecase.GetUserWalletConfigsSetupUseCase
import com.nunchuk.android.utils.onException
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.core.util.orUnknownError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The three choices the owner picks between on the distribution screen. The server has no `BOTH`
 * value — [BOTH] is sent as both claim options — so the tri-state lives here, in the UI layer only.
 */
enum class KeyDistributionChoice {
    SEED_PHRASE_ONLY,
    ENCRYPTED_BACKUP_ONLY,
    BOTH;

    fun toClaimOptions(): List<ClaimOption> = when (this) {
        SEED_PHRASE_ONLY -> listOf(ClaimOption.SEED_PHRASE)
        ENCRYPTED_BACKUP_ONLY -> listOf(ClaimOption.ENCRYPTED_BACKUP)
        BOTH -> listOf(ClaimOption.SEED_PHRASE, ClaimOption.ENCRYPTED_BACKUP)
    }
}

data class KeyDistributionUiState(
    val signer: SignerModel? = null,
    /**
     * Options the device itself supports. A device that cannot encrypt a backup on-device reports
     * `[SEED_PHRASE]` only, which is what collapses this screen to a single option.
     */
    val supportedOptions: List<ClaimOption> = emptyList(),
    /** Server-supplied explanation of the limitation; shown only when an option is unavailable. */
    val claimNote: String? = null,
    /** Null until the server has told us which options exist, so nothing is preselected blindly. */
    val selectedChoice: KeyDistributionChoice? = null,
    /**
     * What the server records against this key — the sharing method chosen, how far each half has
     * been verified, and whether the encrypted backup file has arrived. The same view the key list
     * row renders, so the checklist and the row never disagree.
     */
    val claimState: InheritanceClaimState = InheritanceClaimState(),
    val isLoading: Boolean = false,
) {
    val canUseEncryptedBackup: Boolean
        get() = ClaimOption.ENCRYPTED_BACKUP in supportedOptions

    /** No options means the server has not told us any; the screen has nothing to offer. */
    val isEmpty: Boolean
        get() = supportedOptions.isEmpty()

    /**
     * Whether saving [selectedChoice] would delete the encrypted backup the server is already
     * holding. That is the one irreversible direction, so it needs confirming first.
     */
    val isDroppingEncryptedBackup: Boolean
        get() = ClaimOption.ENCRYPTED_BACKUP in claimState.claimOptions &&
                // Chosen but never uploaded leaves nothing on the server to delete, so there is
                // nothing to warn about.
                claimState.hasEncryptedBackupFile &&
                selectedChoice?.toClaimOptions()?.contains(ClaimOption.ENCRYPTED_BACKUP) == false
}

sealed interface KeyDistributionEvent {
    data object Saved : KeyDistributionEvent
    data class Error(val message: String) : KeyDistributionEvent
}

@HiltViewModel
class KeyDistributionViewModel @Inject constructor(
    private val getUserWalletConfigsSetupFromCacheUseCase: GetUserWalletConfigsSetupFromCacheUseCase,
    private val getUserWalletConfigsSetupUseCase: GetUserWalletConfigsSetupUseCase,
    private val setInheritanceClaimOptionsUseCase: SetInheritanceClaimOptionsUseCase,
    private val syncDraftWalletUseCase: SyncDraftWalletUseCase,
    private val getReplaceWalletStatusUseCase: GetReplaceWalletStatusUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(KeyDistributionUiState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<KeyDistributionEvent>()
    val event = _event.asSharedFlow()

    private var groupId: String = ""
    private var walletId: String = ""
    private var isInitialized: Boolean = false

    /**
     * Whether the owner has picked an option on this run. Until they do, the preselection is
     * derived — from what the server already records for the key, or from the safest option it
     * supports. The two loaders below land in either order, so without this flag the slower one
     * would overwrite a recorded choice with the generic default.
     */
    private var hasUserChosen: Boolean = false

    fun init(
        signer: SignerModel,
        groupId: String,
        walletId: String,
        claimOptions: List<ClaimOption> = emptyList(),
    ) {
        // The activity re-runs this on every recreation; the cache flow never completes, so a second
        // collector would just pile up writing to the same state.
        if (isInitialized) return
        isInitialized = true
        this.groupId = groupId
        this.walletId = walletId
        _state.update {
            it.copy(
                signer = signer,
                // What the caller records for the key, until the server answers with the same
                // plus how far each method has got.
                claimState = it.claimState.copy(claimOptions = claimOptions),
            )
        }
        loadSupportedOptions(signer)
        refreshClaimState()
    }

    /**
     * Re-reads what the server records against this key: the sharing method the owner picked and
     * how far each half of it has been verified. Called again whenever a sub-flow returns, because
     * the verification is recorded over there, not here.
     */
    fun refreshClaimState() {
        val xfp = _state.value.signer?.fingerPrint ?: return
        viewModelScope.launch {
            val signer = if (walletId.isEmpty()) {
                syncDraftWalletUseCase(groupId).getOrNull()
                    ?.signers
                    ?.firstOrNull { it.xfp.equals(xfp, ignoreCase = true) }
            } else {
                // A replacement key never reaches the draft wallet; the server tracks it on the
                // wallet's replacement, keyed by the slot it fills.
                getReplaceWalletStatusUseCase(
                    GetReplaceWalletStatusUseCase.Param(groupId = groupId, walletId = walletId)
                ).getOrNull()
                    ?.signers
                    ?.values
                    ?.firstOrNull { it.xfp.equals(xfp, ignoreCase = true) }
            } ?: return@launch
            _state.update { current ->
                current.copy(
                    claimState = signer.toInheritanceClaimState(),
                    // Coming back to change the method starts from the current one.
                    selectedChoice = current.preselection(
                        recorded = signer.claimOptions,
                        supported = current.supportedOptions,
                    ),
                )
            }
        }
    }

    /**
     * The options come from the server and nowhere else — there is no local default. A device the
     * server says nothing about offers nothing, and the screen shows its empty state rather than
     * inventing a choice the plan may not support.
     */
    private fun loadSupportedOptions(signer: SignerModel) {
        // The cache is empty after a fresh login or cleared data, and nothing else on this screen
        // would ever fill it.
        viewModelScope.launch { getUserWalletConfigsSetupUseCase(Unit) }
        viewModelScope.launch {
            getUserWalletConfigsSetupFromCacheUseCase(Unit)
                .onException { error ->
                    _event.emit(KeyDistributionEvent.Error(error.message.orUnknownError()))
                    _state.update { it.copy(supportedOptions = emptyList(), selectedChoice = null) }
                }
                .collect { result ->
                    result.onFailure { error ->
                        _event.emit(KeyDistributionEvent.Error(error.message.orUnknownError()))
                    }
                    val config = result.getOrNull()
                        ?.supportedSigners
                        ?.filter { it.isInheritanceKey }
                        ?.bestMatch(signer)
                    val supported = config?.claimOptions.orEmpty()
                    _state.update { current ->
                        current.copy(
                            supportedOptions = supported,
                            claimNote = config?.claimNote,
                            selectedChoice = current.preselection(
                                recorded = current.claimState.claimOptions,
                                supported = supported,
                            ),
                        )
                    }
                }
        }
    }

    private fun KeyDistributionUiState.preselection(
        recorded: List<ClaimOption>,
        supported: List<ClaimOption>,
    ): KeyDistributionChoice? = if (hasUserChosen) {
        selectedChoice.coerceTo(supported)
    } else {
        recorded.toChoiceOrNull().coerceTo(supported)
    }

    fun onChoiceSelected(choice: KeyDistributionChoice) {
        hasUserChosen = true
        _state.update { it.copy(selectedChoice = choice) }
    }

    fun onContinueClicked() {
        val signer = _state.value.signer ?: return
        val choice = _state.value.selectedChoice ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            setInheritanceClaimOptionsUseCase(
                SetInheritanceClaimOptionsUseCase.Param(
                    xfp = signer.fingerPrint,
                    claimOptions = choice.toClaimOptions(),
                    groupId = groupId,
                    walletId = walletId,
                )
            ).onSuccess {
                // The checklist opens on this, and it has to show the methods just saved rather
                // than the ones the last refresh knew about.
                _state.update {
                    it.copy(claimState = it.claimState.copy(claimOptions = choice.toClaimOptions()))
                }
                _event.emit(KeyDistributionEvent.Saved)
            }.onFailure {
                _event.emit(KeyDistributionEvent.Error(it.message.orUnknownError()))
            }
            _state.update { it.copy(isLoading = false) }
        }
    }
}

/** The recorded claim options as the tri-state this screen shows; null when nothing is recorded. */
private fun List<ClaimOption>.toChoiceOrNull(): KeyDistributionChoice? = when {
    isEmpty() -> null
    size > 1 -> KeyDistributionChoice.BOTH
    first() == ClaimOption.SEED_PHRASE -> KeyDistributionChoice.SEED_PHRASE_ONLY
    else -> KeyDistributionChoice.ENCRYPTED_BACKUP_ONLY
}

/**
 * Keeps the selection valid for what the server actually offers, preselecting the most protective
 * option it allows. Null when it allows nothing.
 */
private fun KeyDistributionChoice?.coerceTo(
    supported: List<ClaimOption>
): KeyDistributionChoice? = when {
    supported.isEmpty() -> null
    this != null && toClaimOptions().all { it in supported } -> this
    KeyDistributionChoice.BOTH.toClaimOptions().all { it in supported } -> KeyDistributionChoice.BOTH
    ClaimOption.SEED_PHRASE in supported -> KeyDistributionChoice.SEED_PHRASE_ONLY
    else -> KeyDistributionChoice.ENCRYPTED_BACKUP_ONLY
}

/**
 * Picks the entry describing this device. A key does not always carry the same type the server
 * uses to name it — a reused Coldcard is often `AIRGAP` with the `COLDCARD` tag, and a generic
 * air-gapped key has no tag at all — so a tagged entry is preferred and the untagged entry of the
 * same type is the fallback. Being too strict here leaves the owner on the empty state, unable to
 * pick a sharing method at all.
 */
private fun List<SupportedSignerConfig>.bestMatch(signer: SignerModel): SupportedSignerConfig? =
    firstOrNull { config -> config.tagOrNull()?.let { signer.tags.contains(it) } == true }
        ?: firstOrNull { it.typeOrNull() == signer.type && it.tagOrNull() == null }
        ?: firstOrNull { it.typeOrNull() == signer.type }

private fun SupportedSignerConfig.typeOrNull(): SignerType? =
    runCatching { SignerType.valueOf(signerType) }.getOrNull()

private fun SupportedSignerConfig.tagOrNull(): SignerTag? = signerTag?.takeIf { it.isNotBlank() }
    ?.let { runCatching { SignerTag.valueOf(it) }.getOrNull() }
