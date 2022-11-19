package com.nunchuk.android.signer.components.add

import androidx.lifecycle.ViewModel
import com.nunchuk.android.share.membership.MembershipStepManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AirgapIntroViewModel @Inject constructor(
    membershipStepManager: MembershipStepManager
) : ViewModel() {
    val remainTime = membershipStepManager.remainingTime
}

