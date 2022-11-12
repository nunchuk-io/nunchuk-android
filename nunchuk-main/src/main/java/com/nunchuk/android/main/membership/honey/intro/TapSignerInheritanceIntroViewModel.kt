package com.nunchuk.android.main.membership.honey.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.share.membership.MembershipStepManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TapSignerInheritanceIntroViewModel @Inject constructor(
    private val membershipStepManager: MembershipStepManager
) : ViewModel() {
    private val _event = MutableSharedFlow<TapSignerInheritanceIntroEvent>()
    val event = _event.asSharedFlow()

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(TapSignerInheritanceIntroEvent.OnContinueClicked)
        }
    }

    val remainTime = membershipStepManager.remainingTime
}

sealed class TapSignerInheritanceIntroEvent {
    object OnContinueClicked : TapSignerInheritanceIntroEvent()
}