package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.findbackup

import androidx.lifecycle.ViewModel
import com.nunchuk.android.share.membership.MembershipStepManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@HiltViewModel
class FindBackupPasswordViewModel @Inject constructor(
    private val membershipStepManager: MembershipStepManager
) : ViewModel() {
    private val _event = MutableSharedFlow<FindBackupPasswordEvent>()
    val event = _event.asSharedFlow()

    val remainTime = membershipStepManager.remainingTime
}

sealed class FindBackupPasswordEvent