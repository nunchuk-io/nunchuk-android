package com.nunchuk.android.main.groupwallet.keypolicies

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.nunchuk.android.core.mapper.SingleSignerMapper
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.isPlatformKey
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.GroupDummyTransactionPayload
import com.nunchuk.android.model.GroupPlatformKeyPolicies
import com.nunchuk.android.model.byzantine.DummyTransactionType
import com.nunchuk.android.usecase.byzantine.CancelFreeGroupDummyTransactionUseCase
import com.nunchuk.android.usecase.byzantine.GetFreeGroupDummyTransactionUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewGroupKeyPoliciesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getFreeGroupDummyTransactionUseCase: GetFreeGroupDummyTransactionUseCase,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val cancelFreeGroupDummyTransactionUseCase: CancelFreeGroupDummyTransactionUseCase,
    private val singleSignerMapper: SingleSignerMapper,
    private val gson: Gson,
) : ViewModel() {

    private val args = ReviewGroupKeyPoliciesRoute.from(savedStateHandle)

    private val _state = MutableStateFlow(ReviewGroupKeyPoliciesUiState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<ReviewGroupKeyPoliciesEvent>()
    val event = _event.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // Load wallet details for name and signers
            getWalletDetail2UseCase(args.walletId).onSuccess { wallet ->
                val signers = wallet.signers
                    .filter { !it.type.isPlatformKey }
                    .map { singleSignerMapper(it) }
                _state.update {
                    it.copy(
                        walletName = wallet.name,
                        signers = signers,
                    )
                }
            }

            // Load dummy transaction for pending signatures and payload
            getFreeGroupDummyTransactionUseCase(
                GetFreeGroupDummyTransactionUseCase.Param(
                    walletId = args.walletId,
                    dummyTransactionId = args.dummyTransactionId,
                )
            ).onSuccess { dummyTx ->
                _state.update {
                    it.copy(
                        pendingSignatures = dummyTx.pendingSignature,
                        dummyTransactionType = dummyTx.dummyTransactionType,
                    )
                }

                if (dummyTx.dummyTransactionType == DummyTransactionType.UPDATE_PLATFORM_KEY_POLICIES) {
                    parsePlatformKeyPolicyPayload(dummyTx.payload)
                }
            }.onFailure { error ->
                _event.emit(ReviewGroupKeyPoliciesEvent.Error(error.message.orUnknownError()))
            }

            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun parsePlatformKeyPolicyPayload(payload: String) {
        val policyData = runCatching {
            gson.fromJson(payload, GroupDummyTransactionPayload::class.java)
        }.getOrNull() ?: return

        val newPolicies = policyData.newPolicies
        val oldPolicies = policyData.oldPolicies

        val hasPerKeyPolicies = newPolicies.signers.isNotEmpty()
        val policyType =
            if (hasPerKeyPolicies) PolicyType.PER_KEY else PolicyType.GLOBAL

        val newKeyPolicies = if (hasPerKeyPolicies) {
            newPolicies.signers.map { signerPolicy ->
                KeyPolicyItem(
                    fingerPrint = signerPolicy.masterFingerprint,
                    keyPolicy = normalizeGroupPlatformKeyPolicy(signerPolicy.policy),
                )
            }
        } else {
            listOf(
                KeyPolicyItem(
                    keyPolicy = normalizeGroupPlatformKeyPolicy(newPolicies.global),
                )
            )
        }

        val oldKeyPolicies = buildOldKeyPolicies(oldPolicies, hasPerKeyPolicies)

        _state.update {
            it.copy(
                policyType = policyType,
                newPolicies = newKeyPolicies,
                oldPolicies = oldKeyPolicies,
            )
        }
    }

    private fun buildOldKeyPolicies(
        oldPolicies: GroupPlatformKeyPolicies?,
        hasPerKeyPolicies: Boolean,
    ): List<KeyPolicyItem> {
        if (oldPolicies == null) return emptyList()
        return if (hasPerKeyPolicies) {
            oldPolicies.signers.map { signerPolicy ->
                KeyPolicyItem(
                    fingerPrint = signerPolicy.masterFingerprint,
                    keyPolicy = normalizeGroupPlatformKeyPolicy(signerPolicy.policy),
                )
            }
        } else {
            listOf(
                KeyPolicyItem(
                    keyPolicy = normalizeGroupPlatformKeyPolicy(oldPolicies.global),
                )
            )
        }
    }

    fun onContinueClick() {
        viewModelScope.launch {
            _event.emit(
                ReviewGroupKeyPoliciesEvent.OpenWalletAuthentication(
                    walletId = args.walletId,
                    dummyTransactionId = args.dummyTransactionId,
                )
            )
        }
    }

    fun onConfirmDiscard() {
        viewModelScope.launch {
            if (args.dummyTransactionId.isEmpty()) {
                _event.emit(ReviewGroupKeyPoliciesEvent.ConfirmDiscard)
                return@launch
            }
            _state.update { it.copy(isLoading = true) }
            cancelFreeGroupDummyTransactionUseCase(
                CancelFreeGroupDummyTransactionUseCase.Param(
                    walletId = args.walletId,
                    dummyTransactionId = args.dummyTransactionId,
                )
            ).onSuccess {
                _event.emit(ReviewGroupKeyPoliciesEvent.ConfirmDiscard)
            }.onFailure {
                _event.emit(ReviewGroupKeyPoliciesEvent.Error(it.message.orUnknownError()))
            }
            _state.update { it.copy(isLoading = false) }
        }
    }
}

data class ReviewGroupKeyPoliciesUiState(
    val policyType: PolicyType = PolicyType.GLOBAL,
    val signers: List<SignerModel> = emptyList(),
    val newPolicies: List<KeyPolicyItem> = emptyList(),
    val oldPolicies: List<KeyPolicyItem> = emptyList(),
    val pendingSignatures: Int = 0,
    val walletName: String = "",
    val isLoading: Boolean = false,
    val dummyTransactionType: DummyTransactionType = DummyTransactionType.NONE,
)

sealed class ReviewGroupKeyPoliciesEvent {
    data class OpenWalletAuthentication(
        val walletId: String,
        val dummyTransactionId: String,
    ) : ReviewGroupKeyPoliciesEvent()

    data object ConfirmDiscard : ReviewGroupKeyPoliciesEvent()
    data class Error(val message: String) : ReviewGroupKeyPoliciesEvent()
}
