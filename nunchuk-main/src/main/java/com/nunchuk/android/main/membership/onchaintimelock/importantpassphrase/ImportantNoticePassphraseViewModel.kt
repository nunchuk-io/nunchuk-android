package com.nunchuk.android.main.membership.onchaintimelock.importantpassphrase

import androidx.lifecycle.ViewModel
import com.nunchuk.android.share.membership.MembershipStepManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ImportantNoticePassphraseViewModel @Inject constructor(
    private val membershipStepManager: MembershipStepManager,
) : ViewModel() {

    val remainTime = membershipStepManager.remainingTime
}
