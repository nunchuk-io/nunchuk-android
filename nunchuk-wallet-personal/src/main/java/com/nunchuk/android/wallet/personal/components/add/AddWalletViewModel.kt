package com.nunchuk.android.wallet.personal.components.add

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.usecase.GetGlobalGroupWalletConfigUseCase
import com.nunchuk.android.usecase.free.groupwallet.GetGroupSandboxUseCase
import com.nunchuk.android.usecase.free.groupwallet.UpdateGroupSandboxConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AddWalletViewModel @Inject constructor(
    private val getGlobalGroupWalletConfigUseCase: GetGlobalGroupWalletConfigUseCase,
    private val getGroupSandboxUseCase: GetGroupSandboxUseCase,
    private val updateGroupSandboxConfigUseCase: UpdateGroupSandboxConfigUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(AddWalletState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<AddWalletEvent>()
    val event = _event.asSharedFlow()

    private var groupId = ""

    fun init(groupId: String) {
        this.groupId = groupId
        if (groupId.isNotEmpty()) {
            viewModelScope.launch {
                _state.update { it.copy(isLoading = true) }
                getGroupSandboxUseCase(groupId).onSuccess { group ->
                    _state.update {
                        it.copy(
                            groupSandbox = group,
                            addressTypeSelected = group.addressType
                        )
                    }
                    getFreeGroupWalletConfig(group.addressType)
                }
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun getFreeGroupWalletConfig(addressType: AddressType) = viewModelScope.launch {
        viewModelScope.launch {
            getGlobalGroupWalletConfigUseCase(addressType)
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            freeGroupWalletConfig = result
                        )
                    }
                }.onFailure {
                    Timber.e("group-wallet", "Failed to get free group wallet config $it")
                }
        }
    }

    fun updateGroupSandboxConfig(name: String, m: Int, n: Int) = viewModelScope.launch {
        viewModelScope.launch {
            updateGroupSandboxConfigUseCase(
                UpdateGroupSandboxConfigUseCase.Params(
                    groupId = groupId,
                    name = name,
                    m = m,
                    n = n,
                    addressType = state.value.addressTypeSelected
                )
            ).onSuccess { group ->
                _state.update {
                    it.copy(
                        groupSandbox = group
                    )
                }
                _event.emit(AddWalletEvent.UpdateGroupSandboxConfigSuccess(group))
            }.onFailure {
                _event.emit(AddWalletEvent.Error(it.message.orEmpty()))
            }
        }
    }

    fun updateAddressTypeSelected(addressType: AddressType) {
        _state.update {
            it.copy(
                addressTypeSelected = addressType
            )
        }
    }

}