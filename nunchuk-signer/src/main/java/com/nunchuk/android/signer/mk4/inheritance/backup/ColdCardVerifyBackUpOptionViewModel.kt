package com.nunchuk.android.signer.mk4.inheritance.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.inheritance.ClaimOption
import com.nunchuk.android.usecase.membership.SetKeyVerifiedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ColdCardVerifyBackUpOptionViewModel @Inject constructor(
    private val setKeyVerifiedUseCase: SetKeyVerifiedUseCase,
) : ViewModel() {

    private val _event = MutableSharedFlow<ColdCardVerifyBackUpOptionEvent>()
    val event = _event.asSharedFlow()

    /**
     * Records that the owner chose not to verify the encrypted backup.
     *
     * Skipping still settles the artifact — the key list will not keep asking for it — which is
     * why it is written rather than simply closing the flow. [verificationMethod] names which half
     * of a "do both" inheritance key was skipped.
     */
    fun skipVerification(
        groupId: String,
        masterSignerId: String,
        verificationMethod: ClaimOption?,
    ) {
        viewModelScope.launch {
            setKeyVerifiedUseCase(
                SetKeyVerifiedUseCase.Param(
                    groupId = groupId,
                    masterSignerId = masterSignerId,
                    verifyType = VerifyType.SKIPPED_VERIFICATION,
                    verificationMethod = verificationMethod,
                )
            )
            // Either way the flow is done; a failed write leaves the row asking again, which is
            // the honest outcome and better than trapping the owner on this screen.
            _event.emit(ColdCardVerifyBackUpOptionEvent.SkipVerificationHandled)
        }
    }
}

sealed interface ColdCardVerifyBackUpOptionEvent {
    data object SkipVerificationHandled : ColdCardVerifyBackUpOptionEvent
}
