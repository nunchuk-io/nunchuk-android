package com.nunchuk.android.main.membership.onchaintimelock.setuptimelock

import androidx.lifecycle.ViewModel
import com.nunchuk.android.core.ui.TimeZoneDetail
import com.nunchuk.android.share.membership.MembershipStepManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class OnChainSetUpTimelockViewModel @Inject constructor(
    private val membershipStepManager: MembershipStepManager,
) : ViewModel() {

    val remainTime = membershipStepManager.remainingTime
}

data class TimelockData(
    val selectedDate: Calendar,
    val selectedTimeZone: TimeZoneDetail
)