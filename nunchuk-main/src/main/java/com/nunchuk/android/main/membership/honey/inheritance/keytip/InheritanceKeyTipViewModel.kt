package com.nunchuk.android.main.membership.honey.inheritance.keytip

import androidx.lifecycle.ViewModel
import com.nunchuk.android.share.membership.MembershipStepManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@HiltViewModel
class InheritanceKeyTipViewModel @Inject constructor(
    private val membershipStepManager: MembershipStepManager
) : ViewModel() {
    private val _event = MutableSharedFlow<InheritanceKeyTipEvent>()
    val event = _event.asSharedFlow()

    val remainTime = membershipStepManager.remainingTime
}

sealed class InheritanceKeyTipEvent