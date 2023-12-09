package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.requestplanningsent.confirm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.membership.RequestPlanningInheritanceUseCase
import com.nunchuk.android.core.domain.membership.RequestPlanningInheritanceUserDataUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.main.membership.byzantine.groupdashboard.GroupDashboardEvent
import com.nunchuk.android.model.ByzantineMember
import com.nunchuk.android.model.byzantine.AlertType
import com.nunchuk.android.model.byzantine.DummyTransactionPayload
import com.nunchuk.android.usecase.byzantine.DeleteGroupDummyTransactionUseCase
import com.nunchuk.android.usecase.byzantine.GetGroupDummyTransactionPayloadUseCase
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InheritanceRequestPlanningConfirmViewModel @Inject constructor(
    private val requestPlanningInheritanceUserDataUseCase: RequestPlanningInheritanceUserDataUseCase,
    private val requestPlanningInheritanceUseCase: RequestPlanningInheritanceUseCase,
    saveStateHandle: SavedStateHandle,
) : ViewModel() {
    private val args = InheritanceRequestPlanningConfirmFragmentArgs.fromSavedStateHandle(saveStateHandle)
    private val _event = MutableSharedFlow<InheritanceRequestPlanningConfirmEvent>()
    val event = _event.asSharedFlow()

    fun requestInheritancePlanning() = viewModelScope.launch {
        val userData = requestPlanningInheritanceUserDataUseCase(
            RequestPlanningInheritanceUserDataUseCase.Param(
                walletId = args.walletId,
                groupId = args.groupId
            )
        )
        requestPlanningInheritanceUseCase(
            RequestPlanningInheritanceUseCase.Param(
                userData = userData.getOrThrow(),
                walletId = args.walletId,
                groupId = args.groupId
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