package com.nunchuk.android.main.groupwallet.keypolicies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.SetGroupPlatformKeyPoliciesUseCase
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.GroupPlatformKeyPolicies
import com.nunchuk.android.model.GroupPlatformKeyPolicy
import com.nunchuk.android.model.GroupPlatformKeySignerPolicy
import com.nunchuk.android.model.GroupSandbox
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FreeGroupKeyPoliciesViewModel @Inject constructor(
    private val setGroupPlatformKeyPoliciesUseCase: SetGroupPlatformKeyPoliciesUseCase,
) : ViewModel() {

    private var groupId: String = ""
    private var isInitialized = false
    private var allSignerFingerprints: Set<String> = emptySet()
    private var existingPoliciesByFingerprint: Map<String, GroupPlatformKeyPolicy> = emptyMap()

    private val _state = MutableStateFlow(FreeGroupKeyPoliciesUiState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<FreeGroupKeyPoliciesEvent>()
    val event = _event.asSharedFlow()

    fun init(
        groupId: String,
        signers: List<SignerModel>,
        allSigners: List<SignerModel>,
        platformKeyPolicies: GroupPlatformKeyPolicies?,
    ) {
        if (isInitialized) return
        isInitialized = true
        this.groupId = groupId

        val policies = platformKeyPolicies ?: GroupPlatformKeyPolicies()
        allSignerFingerprints = allSigners.mapNotNull { it.fingerPrint.takeIf { xfp -> xfp.isNotEmpty() } }.toSet()
        existingPoliciesByFingerprint = policies.signers
            .filter { it.masterFingerprint.isNotEmpty() }
            .associate { it.masterFingerprint to normalizeGroupPlatformKeyPolicy(it.policy) }
        val hasPerKeyPolicies = policies.signers.isNotEmpty()
        val policyType = if (hasPerKeyPolicies) PolicyType.PER_KEY else PolicyType.GLOBAL

        val keyPolicies = if (hasPerKeyPolicies) {
            signers.map { signer ->
                val signerPolicy = existingPoliciesByFingerprint[signer.fingerPrint]
                KeyPolicyItem(
                    fingerPrint = signer.fingerPrint,
                    derivationPath = signer.derivationPath,
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
                signers = signers,
                policies = keyPolicies,
            )
        }
    }

    fun updateAllSigners(newSigners: List<SignerModel>) {
        allSignerFingerprints = newSigners
            .mapNotNull { it.fingerPrint.takeIf { xfp -> xfp.isNotEmpty() } }
            .toSet()
    }

    fun updateSigners(newSigners: List<SignerModel>) {
        _state.update { state ->
            if (state.signers == newSigners) return@update state
            val updatedPolicies = if (state.policyType == PolicyType.PER_KEY) {
                newSigners.map { signer ->
                    state.policies.firstOrNull {
                        it.fingerPrint == signer.fingerPrint && it.derivationPath == signer.derivationPath
                    } ?: KeyPolicyItem(
                        fingerPrint = signer.fingerPrint,
                        derivationPath = signer.derivationPath,
                        keyPolicy = existingPoliciesByFingerprint[signer.fingerPrint]
                            ?: defaultGroupPlatformKeyPolicy(),
                    )
                }
            } else {
                state.policies
            }
            state.copy(signers = newSigners, policies = updatedPolicies)
        }
    }

    fun changePolicyType(type: PolicyType) {
        _state.update { state ->
            val policies = when (type) {
                PolicyType.GLOBAL -> listOf(KeyPolicyItem())
                PolicyType.PER_KEY -> state.signers.map {
                    KeyPolicyItem(
                        fingerPrint = it.fingerPrint,
                        derivationPath = it.derivationPath,
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
                    if (it.fingerPrint == normalizedPolicy.fingerPrint && it.derivationPath == normalizedPolicy.derivationPath) {
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
            val policies = when (currentState.policyType) {
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
                        val requiredFingerprints = if (allSignerFingerprints.isNotEmpty()) {
                            allSignerFingerprints
                        } else {
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
            setGroupPlatformKeyPoliciesUseCase(
                SetGroupPlatformKeyPoliciesUseCase.Params(
                    groupId = groupId,
                    policies = policies,
                )
            ).onSuccess { groupSandbox ->
                if (currentState.policyType == PolicyType.PER_KEY) {
                    existingPoliciesByFingerprint = policies.signers
                        .associate { it.masterFingerprint to normalizeGroupPlatformKeyPolicy(it.policy) }
                } else {
                    existingPoliciesByFingerprint = emptyMap()
                }
                _state.update { it.copy(hasChanges = false) }
                _event.emit(FreeGroupKeyPoliciesEvent.SaveSuccess(groupSandbox))
            }.onFailure { error ->
                _event.emit(FreeGroupKeyPoliciesEvent.Error(error.message.orUnknownError()))
            }
            _state.update { it.copy(isLoading = false) }
        }
    }
}

data class FreeGroupKeyPoliciesUiState(
    val policyType: PolicyType = PolicyType.GLOBAL,
    val signers: List<SignerModel> = emptyList(),
    val policies: List<KeyPolicyItem> = listOf(KeyPolicyItem()),
    val hasChanges: Boolean = false,
    val isLoading: Boolean = false,
)

data class KeyPolicyItem(
    val fingerPrint: String = "",
    val derivationPath: String = "",
    val keyPolicy: GroupPlatformKeyPolicy = defaultGroupPlatformKeyPolicy(),
)

enum class PolicyType {
    GLOBAL, PER_KEY
}

sealed class FreeGroupKeyPoliciesEvent {
    data class SaveSuccess(val groupSandbox: GroupSandbox) : FreeGroupKeyPoliciesEvent()
    data class Error(val message: String) : FreeGroupKeyPoliciesEvent()
}
