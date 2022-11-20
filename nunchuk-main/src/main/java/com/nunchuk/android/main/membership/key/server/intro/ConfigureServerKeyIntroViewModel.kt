package com.nunchuk.android.main.membership.key.server.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.share.membership.MembershipStepManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfigureServerKeyIntroViewModel @Inject constructor(
    private val membershipStepManager: MembershipStepManager
) : ViewModel() {
    private val _event = MutableSharedFlow<ConfigureServerKeyIntroEvent>()
    val event = _event.asSharedFlow()

    val plan: MembershipPlan
        get() = membershipStepManager.plan

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(ConfigureServerKeyIntroEvent.ContinueStepEvent)
        }
    }
}

sealed class ConfigureServerKeyIntroEvent {
    data class Loading(val isLoading: Boolean) : ConfigureServerKeyIntroEvent()
    object ContinueStepEvent : ConfigureServerKeyIntroEvent()
}