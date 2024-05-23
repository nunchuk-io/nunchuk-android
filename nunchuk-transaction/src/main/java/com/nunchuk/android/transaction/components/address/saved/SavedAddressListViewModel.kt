package com.nunchuk.android.transaction.components.address.saved

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.SavedAddress
import com.nunchuk.android.usecase.membership.GetSavedAddressListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedAddressListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getSavedAddressListUseCase: GetSavedAddressListUseCase
) : ViewModel() {

    private val _event = MutableSharedFlow<SavedAddressListEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(SavedAddressListState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getSavedAddressListUseCase(Unit)
                .onSuccess { addresses ->
                    _state.update { it.copy(addresses = addresses) }
                }.onFailure {
                    _event.emit(SavedAddressListEvent.Error(it.message.orEmpty()))
                }

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