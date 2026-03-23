package com.nunchuk.android.main.groupwallet.keypolicies

import androidx.lifecycle.ViewModel
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.KeyPolicy
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel(assistedFactory = FreeGroupKeyPoliciesViewModel.Factory::class)
class FreeGroupKeyPoliciesViewModel @AssistedInject constructor(
    @Assisted private val signers: List<SignerModel>,
) : ViewModel() {

    private val _state = MutableStateFlow(
        FreeGroupKeyPoliciesUiState(signers = signers)
    )
    val state = _state.asStateFlow()

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
        _state.update { state ->
            state.copy(
                policies = state.policies.map {
                    if (it.fingerPrint == policy.fingerPrint && it.derivationPath == policy.derivationPath) {
                        policy
                    } else {
                        it
                    }
                },
                hasChanges = true,
            )
        }
    }

    fun applyChanges() {
        // TODO: Call use case to save policies to backend
    }

    @AssistedFactory
    interface Factory {
        fun create(signers: List<SignerModel>): FreeGroupKeyPoliciesViewModel
    }
}

data class FreeGroupKeyPoliciesUiState(
    val policyType: PolicyType = PolicyType.GLOBAL,
    val signers: List<SignerModel> = emptyList(),
    val policies: List<KeyPolicyItem> = listOf(KeyPolicyItem()),
    val hasChanges: Boolean = false,
)

data class KeyPolicyItem(
    val fingerPrint: String = "",
    val derivationPath: String = "",
    val keyPolicy: KeyPolicy = KeyPolicy(),
)

enum class PolicyType {
    GLOBAL, PER_KEY
}
