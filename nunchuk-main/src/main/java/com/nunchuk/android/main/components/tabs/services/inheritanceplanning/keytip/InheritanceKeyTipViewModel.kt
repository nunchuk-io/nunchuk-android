package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.keytip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.share.membership.MembershipStepManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InheritanceKeyTipViewModel @Inject constructor(
    private val membershipStepManager: MembershipStepManager
) : ViewModel() {
    private val _event = MutableSharedFlow<InheritanceKeyTipEvent>()
    val event = _event.asSharedFlow()

    val remainTime = membershipStepManager.remainingTime

    fun onContinueClick() = viewModelScope.launch {
        _event.emit(InheritanceKeyTipEvent.ContinueClickEvent)
    }
}

sealed class InheritanceKeyTipEvent {
    object ContinueClickEvent : InheritanceKeyTipEvent()
}