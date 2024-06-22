package com.nunchuk.android.transaction.components.address.saved

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.membership.GetLocalMembershipPlansFlowUseCase
import com.nunchuk.android.model.SavedAddress
import com.nunchuk.android.model.isNonePlan
import com.nunchuk.android.usecase.membership.GetSavedAddressListLocalUseCase
import com.nunchuk.android.usecase.membership.GetSavedAddressListRemoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedAddressListViewModel @Inject constructor(
    private val getSavedAddressListRemoteUseCase: GetSavedAddressListRemoteUseCase,
    private val getSavedAddressListLocalUseCase: GetSavedAddressListLocalUseCase,
    private val getLocalMembershipPlansFlowUseCase: GetLocalMembershipPlansFlowUseCase,
) : ViewModel() {

    private val _event = MutableSharedFlow<SavedAddressListEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(SavedAddressListState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getLocalMembershipPlansFlowUseCase(Unit)
                .map { it.getOrElse { emptyList() } }
                .collect { plans ->
                    if (plans.isNonePlan().not()) {
                        syncAddress()
                    }
                }
        }

        viewModelScope.launch {
            getSavedAddressListLocalUseCase(Unit)
                .collect { result ->
                    _state.update { it.copy(addresses = result.getOrNull().orEmpty()) }
                }
        }
    }

    private fun syncAddress() {
        viewModelScope.launch {
            getSavedAddressListRemoteUseCase(Unit)
        }
    }
}

data class SavedAddressListState(
    val addresses: List<SavedAddress> = arrayListOf(),
)

sealed class SavedAddressListEvent {
    data class Error(val message: String) : SavedAddressListEvent()
    data class Loading(val loading: Boolean) : SavedAddressListEvent()
}