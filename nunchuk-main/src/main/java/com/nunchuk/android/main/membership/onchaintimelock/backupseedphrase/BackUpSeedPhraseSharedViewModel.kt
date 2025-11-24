package com.nunchuk.android.main.membership.onchaintimelock.backupseedphrase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.usecase.membership.SetKeyVerifiedUseCase
import com.nunchuk.android.usecase.membership.SetReplaceKeyVerifiedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackUpSeedPhraseSharedViewModel @Inject constructor(
    private val membershipStepManager: MembershipStepManager,
    private val setKeyVerifiedUseCase: SetKeyVerifiedUseCase,
    private val setReplaceKeyVerifiedUseCase: SetReplaceKeyVerifiedUseCase
) : ViewModel() {

    val remainTime = membershipStepManager.remainingTime

    private val _event = MutableSharedFlow<BackUpSeedPhraseEvent>()
    val event = _event.asSharedFlow()

    fun skipVerification(
        groupId: String,
        masterSignerId: String,
        replacedXfp: String,
        walletId: String
    ) {
        if (masterSignerId.isEmpty()) {
            viewModelScope.launch {
                _event.emit(BackUpSeedPhraseEvent.SkipVerificationError(Exception("Missing required parameters")))
            }
            return
        }

        if (replacedXfp.isEmpty()) {
            viewModelScope.launch {
                val result = setKeyVerifiedUseCase(
                    SetKeyVerifiedUseCase.Param(
                        groupId = groupId,
                        masterSignerId = masterSignerId,
                        verifyType = VerifyType.SKIPPED_VERIFICATION
                    )
                )
                if (result.isSuccess) {
                    _event.emit(BackUpSeedPhraseEvent.SkipVerificationSuccess)
                } else {
                    _event.emit(BackUpSeedPhraseEvent.SkipVerificationError(result.exceptionOrNull()))
                }
            }
        } else {
            setReplaceKeyVerified(keyId = masterSignerId, groupId = groupId, walletId = walletId)
        }
    }

    fun setReplaceKeyVerified(keyId: String, groupId: String, walletId: String) {
        viewModelScope.launch {
            setReplaceKeyVerifiedUseCase(
                SetReplaceKeyVerifiedUseCase.Param(
                    keyId = keyId,
                    checkSum = "",
                    verifyType = VerifyType.SKIPPED_VERIFICATION,
                    groupId = groupId,
                    walletId = walletId
                )
            ).onSuccess {
                _event.emit(BackUpSeedPhraseEvent.SkipVerificationSuccess)
            }.onFailure {
                _event.emit(BackUpSeedPhraseEvent.SkipVerificationError(it))
            }
        }
    }
}

sealed class BackUpSeedPhraseEvent {
    data object SkipVerificationSuccess : BackUpSeedPhraseEvent()
    data class SkipVerificationError(val error: Throwable?) : BackUpSeedPhraseEvent()
}

