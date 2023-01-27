package com.nunchuk.android.share.membership

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.usecase.membership.RestartWizardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MembershipViewModel @Inject constructor(
    private val restartWizardUseCase: RestartWizardUseCase,
) : ViewModel() {

    private val _event = MutableSharedFlow<MembershipEvent>()
    val event = _event.asSharedFlow()

    fun resetWizard(plan: MembershipPlan) {
        viewModelScope.launch {
            val result = restartWizardUseCase(plan)
            if (result.isSuccess) {
                _event.emit(MembershipEvent.RestartWizardSuccess)
            }
        }
    }
}

sealed class MembershipEvent {
    object RestartWizardSuccess : MembershipEvent()
}