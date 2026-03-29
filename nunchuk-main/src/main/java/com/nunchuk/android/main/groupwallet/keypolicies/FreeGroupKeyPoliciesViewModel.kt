package com.nunchuk.android.main.groupwallet.keypolicies

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.DisableGroupPlatformKeyUseCase
import com.nunchuk.android.core.domain.PreviewGroupPlatformKeyPolicyUpdateUseCase
import com.nunchuk.android.core.domain.RequestGroupPlatformKeyPolicyUpdateUseCase
import com.nunchuk.android.core.domain.SetGroupPlatformKeyPoliciesUseCase
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.mapper.SingleSignerMapper
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.isPlatformKey
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.GroupDummyTransaction
import com.nunchuk.android.model.GroupPlatformKeyPolicies
import com.nunchuk.android.model.GroupPlatformKeyPolicy
import com.nunchuk.android.model.GroupPlatformKeySignerPolicy
import com.nunchuk.android.model.GroupSandbox
import com.nunchuk.android.main.R
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.GetGroupWalletConfigUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = FreeGroupKeyPoliciesViewModel.Factory::class)
class FreeGroupKeyPoliciesViewModel @AssistedInject constructor(
    private val disableGroupPlatformKeyUseCase: DisableGroupPlatformKeyUseCase,
    private val setGroupPlatformKeyPoliciesUseCase: SetGroupPlatformKeyPoliciesUseCase,
    private val previewGroupPlatformKeyPolicyUpdateUseCase: PreviewGroupPlatformKeyPolicyUpdateUseCase,
    private val requestGroupPlatformKeyPolicyUpdateUseCase: RequestGroupPlatformKeyPolicyUpdateUseCase,
    private val getGroupWalletConfigUseCase: GetGroupWalletConfigUseCase,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    @Assisted("groupId") private val groupId: String,
    @Assisted("walletId") private val walletId: String,
    @Assisted private val allSigners: List<SignerModel>,
    @Assisted private val platformKeyPolicies: GroupPlatformKeyPolicies?,
    private val singleSignerMapper: SingleSignerMapper,
    private val application: Application,
) : ViewModel() {

    private var allSignerFingerprints: Set<String> = allSigners
        .mapNotNull { it.fingerPrint.takeIf(String::isNotEmpty) }
        .toSet()
    private var existingPoliciesByFingerprint: Map<String, GroupPlatformKeyPolicy> = emptyMap()
    private var pendingPolicies: GroupPlatformKeyPolicies? = null

    private val _state = MutableStateFlow(FreeGroupKeyPoliciesUiState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<FreeGroupKeyPoliciesEvent>()
    val event = _event.asSharedFlow()

    private val isCreatingWallet: Boolean get() = walletId.isEmpty()

    init {
        if (isCreatingWallet) {
            initializeState(allSigners, platformKeyPolicies)
        } else {
            loadWalletData()
        }
    }

    private fun loadWalletData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val walletSigners = getWalletDetail2UseCase(walletId).getOrNull()
                ?.signers
                ?.filter { !it.type.isPlatformKey }
                ?.map { singleSignerMapper(it) }
                .orEmpty()

            val config = getGroupWalletConfigUseCase(walletId).getOrNull()
            val policies = config?.platformKey?.policies

            allSignerFingerprints = walletSigners
                .mapNotNull { it.fingerPrint.takeIf(String::isNotEmpty) }
                .toSet()

            initializeState(walletSigners, policies)
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun initializeState(
        signers: List<SignerModel>,
        platformKeyPolicies: GroupPlatformKeyPolicies?,
    ) {
        val filteredSigners = signers.filter { signer ->
            signer.type != SignerType.PLATFORM
        }
        val policies = platformKeyPolicies ?: GroupPlatformKeyPolicies()
        existingPoliciesByFingerprint = policies.signers
            .filter { it.masterFingerprint.isNotEmpty() }
            .associate { it.masterFingerprint to normalizeGroupPlatformKeyPolicy(it.policy) }
        val hasPerKeyPolicies = policies.signers.isNotEmpty()
        val policyType = if (hasPerKeyPolicies) PolicyType.PER_KEY else PolicyType.GLOBAL

        val keyPolicies = if (hasPerKeyPolicies) {
            filteredSigners.map { signer ->
                val signerPolicy = existingPoliciesByFingerprint[signer.fingerPrint]
                KeyPolicyItem(
                    fingerPrint = signer.fingerPrint,
                    keyPolicy = signerPolicy ?: defaultGroupPlatformKeyPolicy(),
                )
            }
        } else {
            listOf(
                KeyPolicyItem(
                    keyPolicy = normalizeGroupPlatformKeyPolicy(policies.global),
                )
            )
        }

        _state.update {
            it.copy(
                policyType = policyType,
                signers = filteredSigners,
                policies = keyPolicies,
            )
        }
    }

    fun changePolicyType(type: PolicyType) {
        _state.update { state ->
            val policies = when (type) {
                PolicyType.GLOBAL -> listOf(KeyPolicyItem())
                PolicyType.PER_KEY -> state.signers.map {
                    KeyPolicyItem(
                        fingerPrint = it.fingerPrint,
                    )
                }
            }
            state.copy(
                policyType = type,
                policies = policies,
                hasChanges = true,
            )
        }
    }

    fun updatePolicy(policy: KeyPolicyItem) {
        val normalizedPolicy = policy.copy(
            keyPolicy = normalizeGroupPlatformKeyPolicy(policy.keyPolicy)
        )
        _state.update { state ->
            state.copy(
                policies = state.policies.map {
                    if (it.fingerPrint == normalizedPolicy.fingerPrint) {
                        normalizedPolicy
                    } else {
                        it
                    }
                },
                hasChanges = true,
            )
        }
    }

    fun applyChanges() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val currentState = _state.value
            val policies = buildPolicies(currentState)
            if (isCreatingWallet) {
                saveNewWalletPolicies(policies)
            } else {
                previewAndUpdatePolicies(policies)
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun saveNewWalletPolicies(policies: GroupPlatformKeyPolicies) {
        setGroupPlatformKeyPoliciesUseCase(
            SetGroupPlatformKeyPoliciesUseCase.Params(
                groupId = groupId,
                policies = policies,
            )
        ).onSuccess { groupSandbox ->
            updatePoliciesCache(_state.value, policies)
            _state.update { it.copy(hasChanges = false) }
            NcToastManager.scheduleShowMessage(
                application.getString(R.string.nc_platform_key_policies_saved)
            )
            _event.emit(FreeGroupKeyPoliciesEvent.SaveSuccess(groupSandbox))
        }.onFailure { error ->
            _event.emit(FreeGroupKeyPoliciesEvent.Error(error.message.orUnknownError()))
        }
    }

    private suspend fun previewAndUpdatePolicies(policies: GroupPlatformKeyPolicies) {
        previewGroupPlatformKeyPolicyUpdateUseCase(
            PreviewGroupPlatformKeyPolicyUpdateUseCase.Params(
                walletId = walletId,
                policies = policies,
            )
        ).onSuccess { requirement ->
            pendingPolicies = policies
            if (requirement.requiresDummyTransaction) {
                _state.update {
                    it.copy(
                        previewWarning = PreviewWarning(
                            requiresDummyTransaction = true,
                            delayApplyInSeconds = requirement.delayApplyInSeconds,
                        ),
                    )
                }
            } else {
                requestUpdatePolicies(policies)
            }
        }.onFailure { error ->
            _event.emit(FreeGroupKeyPoliciesEvent.Error(error.message.orUnknownError()))
        }
    }

    private suspend fun requestUpdatePolicies(policies: GroupPlatformKeyPolicies) {
        requestGroupPlatformKeyPolicyUpdateUseCase(
            RequestGroupPlatformKeyPolicyUpdateUseCase.Params(
                walletId = walletId,
                policies = policies,
            )
        ).onSuccess {
            updatePoliciesCache(_state.value, policies)
            _event.emit(FreeGroupKeyPoliciesEvent.UpdatePolicySuccess)
        }.onFailure { error ->
            _event.emit(FreeGroupKeyPoliciesEvent.Error(error.message.orUnknownError()))
        }
    }

    fun dismissPreviewWarning() {
        _state.update { it.copy(previewWarning = null) }
    }

    fun confirmApplyChanges() {
        val policies = pendingPolicies ?: return
        _state.update { it.copy(previewWarning = null) }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            requestGroupPlatformKeyPolicyUpdateUseCase(
                RequestGroupPlatformKeyPolicyUpdateUseCase.Params(
                    walletId = walletId,
                    policies = policies,
                )
            ).onSuccess { requirement ->
                updatePoliciesCache(_state.value, policies)
                pendingPolicies = null
                _event.emit(
                    FreeGroupKeyPoliciesEvent.OpenWalletAuthentication(
                        walletId = walletId,
                        dummyTransaction = requirement.dummyTransaction,
                    )
                )
            }.onFailure { error ->
                _event.emit(FreeGroupKeyPoliciesEvent.Error(error.message.orUnknownError()))
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun buildPolicies(currentState: FreeGroupKeyPoliciesUiState): GroupPlatformKeyPolicies {
        return when (currentState.policyType) {
            PolicyType.GLOBAL -> GroupPlatformKeyPolicies(
                global = normalizeGroupPlatformKeyPolicy(
                    currentState.policies.firstOrNull()?.keyPolicy
                ),
                signers = emptyList(),
            )

            PolicyType.PER_KEY -> GroupPlatformKeyPolicies(
                global = null,
                signers = run {
                    val policyByFingerprint = currentState.policies
                        .filter { it.fingerPrint.isNotEmpty() }
                        .associate {
                            it.fingerPrint to normalizeGroupPlatformKeyPolicy(it.keyPolicy)
                        }
                    val requiredFingerprints = allSignerFingerprints.ifEmpty {
                        policyByFingerprint.keys
                    }
                    requiredFingerprints.sorted().map { fingerprint ->
                        GroupPlatformKeySignerPolicy(
                            masterFingerprint = fingerprint,
                            policy = policyByFingerprint[fingerprint]
                                ?: existingPoliciesByFingerprint[fingerprint]
                                ?: defaultGroupPlatformKeyPolicy(),
                        )
                    }
                },
            )
        }
    }

    private fun updatePoliciesCache(
        currentState: FreeGroupKeyPoliciesUiState,
        policies: GroupPlatformKeyPolicies,
    ) {
        existingPoliciesByFingerprint = if (currentState.policyType == PolicyType.PER_KEY) {
            policies.signers
                .associate { it.masterFingerprint to normalizeGroupPlatformKeyPolicy(it.policy) }
        } else {
            emptyMap()
        }
    }

    fun disablePlatformKey() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            disableGroupPlatformKeyUseCase(groupId).onSuccess { groupSandbox ->
                _state.update { it.copy(hasChanges = false) }
                _event.emit(FreeGroupKeyPoliciesEvent.SaveSuccess(groupSandbox))
            }.onFailure { error ->
                _event.emit(FreeGroupKeyPoliciesEvent.Error(error.message.orUnknownError()))
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("groupId") groupId: String,
            @Assisted("walletId") walletId: String,
            allSigners: List<SignerModel>,
            platformKeyPolicies: GroupPlatformKeyPolicies?,
        ): FreeGroupKeyPoliciesViewModel
    }
}

data class FreeGroupKeyPoliciesUiState(
    val policyType: PolicyType = PolicyType.GLOBAL,
    val signers: List<SignerModel> = emptyList(),
    val policies: List<KeyPolicyItem> = listOf(KeyPolicyItem()),
    val hasChanges: Boolean = false,
    val isLoading: Boolean = false,
    val previewWarning: PreviewWarning? = null,
)

data class PreviewWarning(
    val requiresDummyTransaction: Boolean = false,
    val delayApplyInSeconds: Int = 0,
)

data class KeyPolicyItem(
    val fingerPrint: String = "",
    val keyPolicy: GroupPlatformKeyPolicy = defaultGroupPlatformKeyPolicy(),
)

enum class PolicyType {
    GLOBAL, PER_KEY
}

sealed class FreeGroupKeyPoliciesEvent {
    data class SaveSuccess(val groupSandbox: GroupSandbox) : FreeGroupKeyPoliciesEvent()
    data object UpdatePolicySuccess : FreeGroupKeyPoliciesEvent()
    data class Error(val message: String) : FreeGroupKeyPoliciesEvent()
    data class OpenWalletAuthentication(
        val walletId: String,
        val dummyTransaction: GroupDummyTransaction?,
    ) : FreeGroupKeyPoliciesEvent()
}
