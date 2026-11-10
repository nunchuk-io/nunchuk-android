package com.nunchuk.android.main.membership.backupseedphrase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.signer.OnChainVerifyKeyResolver
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.skipVerifyType
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.inheritance.ClaimOption
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.usecase.membership.RestartWizardUseCase
import com.nunchuk.android.usecase.membership.SetKeyVerifiedUseCase
import com.nunchuk.android.usecase.membership.SetReplaceKeyVerifiedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackUpSeedPhraseSharedViewModel @Inject constructor(
    private val membershipStepManager: MembershipStepManager,
    private val setKeyVerifiedUseCase: SetKeyVerifiedUseCase,
    private val setReplaceKeyVerifiedUseCase: SetReplaceKeyVerifiedUseCase,
    private val restartWizardUseCase: RestartWizardUseCase,
    private val onChainVerifyKeyResolver: OnChainVerifyKeyResolver,
) : ViewModel() {

    val remainTime = membershipStepManager.remainingTime

    private val _state = MutableStateFlow(BackUpSeedPhraseUiState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<BackUpSeedPhraseEvent>()
    val event = _event.asSharedFlow()

    /**
     * The re-added key derived a different public key. This is state rather than a one-shot
     * event: the screen it drives stays up until the owner acts on it, and it has to survive the
     * host being stopped while the device screen is in front.
     */
    fun onReAddedKeyMismatched(signer: SignerModel) {
        _state.update { it.copy(mismatchedSigner = signer) }
    }

    fun onMismatchHandled() {
        _state.update { it.copy(mismatchedSigner = null) }
    }

    /**
     * [verificationMethod] names which sharing method of an off-chain inheritance key is being
     * skipped. Null on the on-chain timelock flow, which tracks one verification per key.
     */
    fun skipVerification(
        groupId: String,
        masterSignerId: String,
        replacedXfp: String,
        walletId: String,
        verificationMethod: ClaimOption? = null,
    ) {
        if (masterSignerId.isEmpty()) {
            viewModelScope.launch {
                _event.emit(BackUpSeedPhraseEvent.SkipVerificationError(Exception("Missing required parameters")))
            }
            return
        }

        if (replacedXfp.isEmpty()) {
            viewModelScope.launch {
                // A configurable on-chain draft cannot skip: the key is marked verified instead.
                val result = onChainVerifyKeyResolver.draftKey(groupId, masterSignerId).mapCatching { recoveredKey ->
                    setKeyVerifiedUseCase(
                        SetKeyVerifiedUseCase.Param(
                            groupId = groupId,
                            masterSignerId = masterSignerId,
                            verifyType = recoveredKey.skipVerifyType(),
                            verificationMethod = verificationMethod,
                            recoveredKey = recoveredKey,
                        )
                    ).getOrThrow()
                }
                if (result.isSuccess) {
                    _event.emit(BackUpSeedPhraseEvent.SkipVerificationSuccess)
                } else {
                    _event.emit(BackUpSeedPhraseEvent.SkipVerificationError(result.exceptionOrNull()))
                }
            }
        } else {
            setReplaceKeyVerified(
                keyId = masterSignerId,
                groupId = groupId,
                walletId = walletId,
                verificationMethod = verificationMethod,
            )
        }
    }

    fun setReplaceKeyVerified(
        keyId: String,
        groupId: String,
        walletId: String,
        verificationMethod: ClaimOption? = null,
    ) {
        viewModelScope.launch {
            // Same for a replacement on a configurable wallet.
            onChainVerifyKeyResolver.replacementKey(groupId, walletId, keyId).mapCatching { recoveredKey ->
                setReplaceKeyVerifiedUseCase(
                    SetReplaceKeyVerifiedUseCase.Param(
                        keyId = keyId,
                        checkSum = "",
                        verifyType = recoveredKey.skipVerifyType(),
                        groupId = groupId,
                        walletId = walletId,
                        verificationMethod = verificationMethod,
                        recoveredKey = recoveredKey,
                    )
                ).getOrThrow()
            }.onSuccess {
                _event.emit(BackUpSeedPhraseEvent.SkipVerificationSuccess)
            }.onFailure {
                _event.emit(BackUpSeedPhraseEvent.SkipVerificationError(it))
            }
        }
    }

    fun resetWizard(plan: MembershipPlan, groupId: String) {
        viewModelScope.launch {
            restartWizardUseCase(RestartWizardUseCase.Param(plan, groupId))
                .onSuccess {
                    membershipStepManager.restart()
                    _event.emit(BackUpSeedPhraseEvent.RestartWizardSuccess)
                }.onFailure {
                    _event.emit(BackUpSeedPhraseEvent.Error(it.message.orUnknownError()))
                }
        }
    }
}

data class BackUpSeedPhraseUiState(
    /** The key that was re-added but does not match the one being verified. */
    val mismatchedSigner: SignerModel? = null,
)

sealed class BackUpSeedPhraseEvent {
    data object SkipVerificationSuccess : BackUpSeedPhraseEvent()
    data class SkipVerificationError(val error: Throwable?) : BackUpSeedPhraseEvent()
    data object RestartWizardSuccess : BackUpSeedPhraseEvent()
    data class Error(val message: String) : BackUpSeedPhraseEvent()
}

