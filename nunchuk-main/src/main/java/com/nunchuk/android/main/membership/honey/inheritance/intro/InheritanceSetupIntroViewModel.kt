package com.nunchuk.android.main.membership.honey.inheritance.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.share.membership.MembershipStepManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InheritanceSetupIntroViewModel @Inject constructor(
    private val membershipStepManager: MembershipStepManager,
) : ViewModel() {
    private val _event = MutableSharedFlow<InheritanceSetupIntroEvent>()
    val event = _event.asSharedFlow()

    val remainTime = membershipStepManager.remainingTime

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(InheritanceSetupIntroEvent.OnContinueClicked)
        }
    }
}

sealed class InheritanceSetupIntroEvent {
    object OnContinueClicked : InheritanceSetupIntroEvent()
}