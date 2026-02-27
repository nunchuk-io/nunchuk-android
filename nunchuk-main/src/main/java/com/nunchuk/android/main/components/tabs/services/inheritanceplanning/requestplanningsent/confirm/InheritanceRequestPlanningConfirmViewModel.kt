package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.requestplanningsent.confirm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.membership.RequestPlanningInheritanceUseCase
import com.nunchuk.android.core.domain.membership.RequestPlanningInheritanceUserDataUseCase
import com.nunchuk.android.core.util.orUnknownError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InheritanceRequestPlanningConfirmViewModel @Inject constructor(
    private val requestPlanningInheritanceUserDataUseCase: RequestPlanningInheritanceUserDataUseCase,
    private val requestPlanningInheritanceUseCase: RequestPlanningInheritanceUseCase,
) : ViewModel() {
    private val _event = MutableSharedFlow<InheritanceRequestPlanningConfirmEvent>()
    val event = _event.asSharedFlow()

    fun requestInheritancePlanning(walletId: String, groupId: String) = viewModelScope.launch {
        val userData = requestPlanningInheritanceUserDataUseCase(
            RequestPlanningInheritanceUserDataUseCase.Param(
                walletId = walletId,
                groupId = groupId
            )
        )
        requestPlanningInheritanceUseCase(
            RequestPlanningInheritanceUseCase.Param(
                userData = userData.getOrThrow(),
                walletId = walletId,
                groupId = groupId
            )
        ).onSuccess {
            _event.emit(
                InheritanceRequestPlanningConfirmEvent.RequestInheritanceSuccess
            )
        }.onFailure {
            _event.emit(InheritanceRequestPlanningConfirmEvent.Error(it.message.orUnknownError()))
        }
    }
}

sealed class InheritanceRequestPlanningConfirmEvent {
    data class Loading(val isLoading: Boolean) : InheritanceRequestPlanningConfirmEvent()
    data class Error(val message: String) : InheritanceRequestPlanningConfirmEvent()
    data object RequestInheritanceSuccess : InheritanceRequestPlanningConfirmEvent()
}
