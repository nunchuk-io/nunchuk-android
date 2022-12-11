package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.keytip.InheritanceKeyTipEvent
import com.nunchuk.android.share.membership.MembershipStepManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InheritanceClaimViewModel @Inject constructor() : ViewModel() {
    private val _event = MutableSharedFlow<InheritanceClaimEvent>()
    val event = _event.asSharedFlow()

    fun onContinueClick() = viewModelScope.launch {
        _event.emit(InheritanceClaimEvent.ContinueClickEvent)
    }
}

sealed class InheritanceClaimEvent {
    object ContinueClickEvent : InheritanceClaimEvent()
}