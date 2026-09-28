package com.nunchuk.android.signer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.settings.GetChainSettingFlowUseCase
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.signer.SignerIntroFlow
import com.nunchuk.android.core.signer.SignerIntroRequest
import com.nunchuk.android.core.util.isRecommendedMultiSigPath
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.CreateSoftwareSignerUseCase
import com.nunchuk.android.usecase.DeleteMasterSignerUseCase
import com.nunchuk.android.usecase.GetMasterFingerprintUseCase
import com.nunchuk.android.usecase.GetUserWalletConfigsSetupFromCacheUseCase
import com.nunchuk.android.usecase.GetUserWalletConfigsSetupUseCase
import com.nunchuk.android.usecase.membership.RestartWizardUseCase
import com.nunchuk.android.usecase.signer.CreateSoftwareSignerByXprvUseCase
import com.nunchuk.android.usecase.signer.GetAllSignersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class SignerIntroState(
    val existingSelection: ExistingSignerSelection? = null,
    val showRecoveryOptions: Boolean = false,
    /** The user's already-imported signers, used to offer "reuse existing" vs "set up new". */
    val allSigners: List<SignerModel> = emptyList(),
    /** All signer types supported for the wallet type being built. */
    val supportedSigners: List<SupportedSigner> = emptyList(),
    /**
     * Subset of [supportedSigners] eligible for the specific key being added: backend configs
     * whose inheritance flag matches the current slot (isInheritanceKey == isAddInheritanceSigner).
     * Empty unless the on-chain, config-driven flow is active; takes priority over [supportedSigners]
     * when non-empty.
     */
    val eligibleSupportedSigners: List<SupportedSigner> = emptyList(),
    val signerDisplayInfos: List<SignerDisplayInfo> = emptyList(),
)

@HiltViewModel
class SignerIntroViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val membershipStepManager: MembershipStepManager,
    private val getAllSignersUseCase: GetAllSignersUseCase,
    private val masterSignerMapper: MasterSignerMapper,
    private val getUserWalletConfigsSetupFromCacheUseCase: GetUserWalletConfigsSetupFromCacheUseCase,
    private val getUserWalletConfigsSetupUseCase: GetUserWalletConfigsSetupUseCase,
    private val restartWizardUseCase: RestartWizardUseCase,
    private val getChainSettingFlowUseCase: GetChainSettingFlowUseCase,
    private val createSoftwareSignerUseCase: CreateSoftwareSignerUseCase,
    private val createSoftwareSignerByXprvUseCase: CreateSoftwareSignerByXprvUseCase,
    private val getMasterFingerprintUseCase: GetMasterFingerprintUseCase,
    private val deleteMasterSignerUseCase: DeleteMasterSignerUseCase,
) : ViewModel() {

    val remainTime = membershipStepManager.remainingTime

    private var onChainAddSignerParam: OnChainAddSignerParam? = null
    private var isTestNet: Boolean = false
    private var pickerPolicy: SignerIntroPickerPolicy? = null

    private val _state = MutableStateFlow(SignerIntroState())
    val state = _state.asStateFlow()

    private val _event = Channel<SignerIntroEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()
    private var coordinator: SignerIntroCoordinator? = null
    private val selectionState = SignerIntroSelectionState(savedStateHandle)
    val verifyingKeyType: KeyType? get() = requireNotNull(coordinator).verifyingKeyType
    val isInheritanceSetup: Boolean get() = requireNotNull(coordinator).isInheritanceSetup

    init {
        viewModelScope.launch {
            getChainSettingFlowUseCase(Unit)
                .map { it.getOrDefault(Chain.MAIN) }
                .collect {
                    isTestNet = it == Chain.TESTNET
                }
        }
    }

    fun init(request: SignerIntroRequest) {
        if (coordinator != null) return
        val routing = SignerIntroCoordinator(request)
        coordinator = routing
        onChainAddSignerParam = request.deviceParams
        val policy = SignerIntroPickerPolicy(request)
        pickerPolicy = policy
        _state.update { it.copy(supportedSigners = policy.initialSigners) }
        if (policy.usesWalletConfigs) fetchUserWalletConfigs()
        if (policy.loadsExistingSigners) loadAllSigners()
        updateSignerDisplayInfos()
        _state.update { it.copy(showRecoveryOptions = savedStateHandle[RECOVERY_VISIBLE] ?: false) }
        if (routing.verifyingKeyType != null && savedStateHandle.get<Boolean>(VERIFICATION_OPENED) != true) {
            savedStateHandle[VERIFICATION_OPENED] = true
            onKeySelected(routing.verifyingKeyType)
        }
    }

    private fun updateSignerDisplayInfos() {
        val policy = requireNotNull(pickerPolicy)
        _state.update { it.copy(signerDisplayInfos = policy.displayInfos(it)) }
    }

    private fun fetchUserWalletConfigs() {
        viewModelScope.launch {
            getUserWalletConfigsSetupFromCacheUseCase(Unit).collect { result ->
                result.getOrNull()?.let { walletConfigs ->
                    _state.update { requireNotNull(pickerPolicy).applyConfigs(it, walletConfigs.supportedSigners) }
                    updateSignerDisplayInfos()
                }
            }
        }
        viewModelScope.launch {
            getUserWalletConfigsSetupUseCase(Unit)
        }
    }

    private fun loadAllSigners() {
        viewModelScope.launch {
            getAllSignersUseCase(true).onSuccess { (masterSigners, singleSigners) ->
                _state.update { it.copy(allSigners = mapSigners(singleSigners, masterSigners)) }
                // Restoration may reopen a sheet, but must never launch a new device flow.
                val restored = selectionState.restore(::findExistingSigners)
                _state.update { it.copy(existingSelection = restored) }
            }
        }
    }

    private suspend fun mapSigners(
        singleSigners: List<SingleSigner>,
        masterSigners: List<MasterSigner>
    ): List<SignerModel> {
        return masterSigners.map { masterSignerMapper(it) } +
                singleSigners.map(SingleSigner::toModel)
    }

    private fun filterSignerByType(type: SignerType, tag: SignerTag? = null): List<SignerModel> {
        return state.value.allSigners.filter { signer ->
            when {
                tag == null -> signer.type == type
                // A Coldcard is either its own type or an air-gapped key carrying the tag, so here
                // the tag widens the match.
                tag == SignerTag.COLDCARD -> signer.type == type || signer.tags.contains(tag)
                // Every other tag is what tells devices of the same type apart — Ledger/Trezor/
                // BitBox all being HARDWARE, Jade/Keystone/Passport all being AIRGAP — so it has to
                // narrow the match. Widening here offers a Trezor when the owner picked BitBox.
                else -> signer.type == type && signer.tags.contains(tag)
            }
        }
    }

    fun onKeySelected(keyType: KeyType) {
        dispatch(requireNotNull(coordinator).select(keyType))
    }

    fun onFirmwareChecked(device: SignerFirmwareDevice) {
        dispatch(requireNotNull(coordinator).firmwareChecked(device))
    }

    fun onCreateNewSigner() {
        val keyType = state.value.existingSelection?.keyType ?: return
        dismissExistingSigners()
        dispatch(requireNotNull(coordinator).createNew(keyType))
    }

    fun dismissExistingSigners() {
        selectionState.clear()
        _state.update { it.copy(existingSelection = null) }
    }

    fun dismissRecoveryOptions() {
        savedStateHandle[RECOVERY_VISIBLE] = false
        _state.update { it.copy(showRecoveryOptions = false) }
    }

    private fun dispatch(action: SignerIntroAction) {
        when (action) {
            is SignerIntroAction.OfferExisting -> offerExisting(action.keyType)
            SignerIntroAction.RecoverSoftware -> {
                savedStateHandle[RECOVERY_VISIBLE] = true
                _state.update { it.copy(showRecoveryOptions = true) }
            }
            is SignerDeviceAction -> sendEvent(SignerIntroEvent.OpenDevice(action))
            is SignerIntroAction.CheckFirmware -> sendEvent(SignerIntroEvent.CheckFirmware(action.device))
            is SignerIntroAction.ReturnHardwareTag -> sendEvent(SignerIntroEvent.ReturnHardwareTag(action.tag))
            SignerIntroAction.ReturnPlatformKey -> sendEvent(SignerIntroEvent.ReturnPlatformKey)
        }
    }

    private fun sendEvent(event: SignerIntroEvent) {
        viewModelScope.launch { _event.send(event) }
    }

    private fun findExistingSigners(keyType: KeyType): List<SignerModel> {
        val (type, deviceTag) = keyType.toSignerTypeAndTag()
        // Claims also accept a Coldcard previously imported through QR/file.
        val tag = if (keyType == KeyType.COLDCARD &&
            coordinator?.request?.flow is SignerIntroFlow.ClaimInheritance
        ) SignerTag.COLDCARD else deviceTag
        return filterExistingSigners(
            filterSignerByType(type, tag).filter { it.derivationPath.isRecommendedMultiSigPath }
        )
    }

    private fun offerExisting(keyType: KeyType) {
        val signers = findExistingSigners(keyType)
        if (signers.isEmpty()) {
            dismissExistingSigners()
            dispatch(requireNotNull(coordinator).createNew(keyType))
        } else {
            selectionState.save(keyType)
            _state.update { it.copy(existingSelection = ExistingSignerSelection(keyType, signers)) }
        }
    }

    private fun filterExistingSigners(signers: List<SignerModel>): List<SignerModel> {
        val existingSigners = onChainAddSignerParam?.existingSigners ?: return signers
        if (existingSigners.isEmpty()) return signers
        return signers.filter { signer ->
            val signerDerivationPath = signer.derivationPath.ifEmpty {
                getPath(index = if (signer.index <= 0) 0 else signer.index)
            }
            existingSigners.none { existing ->
                val existingDerivationPath = existing.derivationPath.ifEmpty {
                    getPath(index = if (existing.index <= 0) 0 else existing.index)
                }
                existing.fingerPrint == signer.fingerPrint && existingDerivationPath == signerDerivationPath
            }
        }
    }

    private fun getPath(index: Int): String {
        return if (isTestNet) "m/48h/1h/${index}h/2h" else "m/48h/0h/${index}h/2h"
    }

    fun resetWizard(plan: MembershipPlan, groupId: String) {
        viewModelScope.launch {
            restartWizardUseCase(RestartWizardUseCase.Param(plan, groupId))
                .onSuccess {
                    membershipStepManager.restart()
                    _event.send(SignerIntroEvent.RestartWizardSuccess)
                }.onFailure {
                    _event.send(SignerIntroEvent.Error(it.message.orUnknownError()))
                }
        }
    }

    fun createSoftwareSignerFromMnemonic(mnemonic: String, passphrase: String, signerName: String) {
        viewModelScope.launch {
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
                    replace = true
                )
            ).onSuccess { signer ->
                _event.send(SignerIntroEvent.CreateSoftwareSignerSuccess(masterSignerMapper(signer)))
            }.onFailure { e ->
                Timber.e(e)
                _event.send(SignerIntroEvent.Error(e.message.orUnknownError()))
            }
        }
    }

    fun createSoftwareSignerFromXprv(xprv: String, signerName: String) {
        viewModelScope.launch {
            createSoftwareSignerByXprvUseCase(
                CreateSoftwareSignerByXprvUseCase.Param(
                    name = signerName,
                    xprv = xprv,
                    replace = true
                )
            ).onSuccess { signer ->
                _event.send(SignerIntroEvent.CreateSoftwareSignerSuccess(masterSignerMapper(signer)))
            }.onFailure { e ->
                Timber.e(e)
                _event.send(SignerIntroEvent.Error(e.message.orUnknownError()))
            }
        }
    }
    private companion object {
        const val RECOVERY_VISIBLE = "signer_intro_recovery_visible"
        const val VERIFICATION_OPENED = "signer_intro_verification_opened"
    }

}

sealed interface SignerIntroEvent {
    data class CheckFirmware(val device: SignerFirmwareDevice) : SignerIntroEvent
    data class OpenDevice(val action: SignerDeviceAction) : SignerIntroHostEvent
    data class ReturnHardwareTag(val tag: SignerTag) : SignerIntroHostEvent
    data object ReturnPlatformKey : SignerIntroHostEvent
    data object RestartWizardSuccess : SignerIntroHostEvent
    data class Error(val message: String) : SignerIntroHostEvent
    data class CreateSoftwareSignerSuccess(val signer: SignerModel) : SignerIntroHostEvent
}

/** The Activity cannot receive a Compose navigation or sheet action. */
sealed interface SignerIntroHostEvent : SignerIntroEvent
