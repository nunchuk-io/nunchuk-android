package com.nunchuk.android.transaction.components.address.addoredit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.membership.GetLocalMembershipPlansFlowUseCase
import com.nunchuk.android.core.util.SavedAddressFlow
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.SavedAddress
import com.nunchuk.android.model.isNonePlan
import com.nunchuk.android.usecase.CheckAddressValidUseCase
import com.nunchuk.android.usecase.ParseBtcUriUseCase
import com.nunchuk.android.usecase.membership.AddOrUpdateSavedAddressUseCase
import com.nunchuk.android.usecase.membership.DeleteSavedAddressUseCase
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
class AddOrEditAddressViewModel @Inject constructor(
    private val parseBtcUriUseCase: ParseBtcUriUseCase,
    private val addOrUpdateSavedAddressUseCase: AddOrUpdateSavedAddressUseCase,
    private val deleteSavedAddressUseCase: DeleteSavedAddressUseCase,
    private val checkAddressValidUseCase: CheckAddressValidUseCase,
    private val getLocalMembershipPlansFlowUseCase: GetLocalMembershipPlansFlowUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val args = AddOrEditAddressFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<AddOrEditAddressEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(AddOrEditAddressState())
    val state = _state.asStateFlow()

    init {
        if (args.flow == SavedAddressFlow.EDIT) {
            _state.update {
                it.copy(
                    address = args.address?.address.orEmpty(),
                    label = args.address?.label.orEmpty(),
                    originSavedAddress = args.address
                )
            }
        }
        viewModelScope.launch {
            getLocalMembershipPlansFlowUseCase(Unit)
                .map { it.getOrElse { emptyList() } }
                .collect { plans ->
                    _state.update { it.copy(isPremiumUser = plans.isNonePlan().not()) }
                }
        }
    }

    fun parseBtcUri(content: String) {
        viewModelScope.launch {
            val result = parseBtcUriUseCase(content)
            if (result.isSuccess) {
                val btcUri = result.getOrThrow()
                _state.update { it.copy(address = btcUri.address) }
            } else {
                _event.emit(AddOrEditAddressEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun addOrUpdateAddress() {
        viewModelScope.launch {
            val resultCheckAddress =
                checkAddressValidUseCase(CheckAddressValidUseCase.Params(listOf(state.value.address)))
            if (resultCheckAddress.isSuccess && resultCheckAddress.getOrThrow().isEmpty()) {
                _state.update { it.copy(invalidAddress = false) }
            } else {
                _state.update { it.copy(invalidAddress = true) }
                return@launch
            }
            _event.emit(AddOrEditAddressEvent.Loading(true))
            val result = addOrUpdateSavedAddressUseCase(
                AddOrUpdateSavedAddressUseCase.Params(
                    address = state.value.address,
                    label = state.value.label,
                    isPremiumUser = state.value.isPremiumUser
                )
            )
            if (result.isSuccess) {
                _event.emit(AddOrEditAddressEvent.Success(state.value.address, state.value.label))
            } else {
                _event.emit(AddOrEditAddressEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
            }
            _event.emit(AddOrEditAddressEvent.Loading(false))
        }
    }

    fun deleteAddress() {
        if (args.address == null) return
        viewModelScope.launch {
            _event.emit(AddOrEditAddressEvent.Loading(true))
            val result =
                deleteSavedAddressUseCase(DeleteSavedAddressUseCase.Params(args.address!!.address, _state.value.isPremiumUser))
            if (result.isSuccess) {
                _event.emit(AddOrEditAddressEvent.Success(state.value.address, state.value.label))
            } else {
                _event.emit(AddOrEditAddressEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
            }
            _event.emit(AddOrEditAddressEvent.Loading(false))
        }
    }

    fun updateAddress(address: String) {
        _state.update { it.copy(address = address) }
    }

    fun updateLabel(label: String) {
        _state.update { it.copy(label = label) }
    }
}

data class AddOrEditAddressState(
    val address: String = "",
    val label: String = "",
    val originSavedAddress: SavedAddress? = null,
    val invalidAddress: Boolean = false,
    val isPremiumUser: Boolean = false,
)

sealed class AddOrEditAddressEvent {
    data class Error(val message: String) : AddOrEditAddressEvent()
    data class Loading(val loading: Boolean) : AddOrEditAddressEvent()
    data class Success(val address: String, val label: String) : AddOrEditAddressEvent()
}