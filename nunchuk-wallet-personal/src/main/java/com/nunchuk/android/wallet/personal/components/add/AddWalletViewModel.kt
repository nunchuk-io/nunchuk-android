package com.nunchuk.android.wallet.personal.components.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.usecase.ClearMiniscriptLocalUseCase
import com.nunchuk.android.usecase.GetGlobalGroupWalletConfigUseCase
import com.nunchuk.android.usecase.GetMiniscriptLocalUseCase
import com.nunchuk.android.usecase.SetMiniscriptLocalUseCase
import com.nunchuk.android.usecase.free.groupwallet.GetGroupSandboxUseCase
import com.nunchuk.android.usecase.free.groupwallet.UpdateGroupSandboxConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AddWalletViewModel @Inject constructor(
    private val getGlobalGroupWalletConfigUseCase: GetGlobalGroupWalletConfigUseCase,
    private val getGroupSandboxUseCase: GetGroupSandboxUseCase,
    private val updateGroupSandboxConfigUseCase: UpdateGroupSandboxConfigUseCase,
    private val getMiniscriptLocalUseCase: GetMiniscriptLocalUseCase,
    private val setMiniscriptLocalUseCase: SetMiniscriptLocalUseCase,
    private val clearMiniscriptLocalUseCase: ClearMiniscriptLocalUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AddWalletState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<AddWalletEvent>()
    val event = _event.asSharedFlow()

    private var groupId = ""

    init {
        // Observe miniscriptLocal and update state
        viewModelScope.launch {
            getMiniscriptLocalUseCase(Unit).map { it.getOrDefault("") }.collect { miniscriptLocal ->
                _state.update { it.copy(miniscriptLocal = miniscriptLocal) }
            }
        }
    }

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

    fun selectAddressType(addressType: AddressType) {
        _state.update { it.copy(addressTypeSelected = addressType) }
    }

    fun updateWalletConfig(
        walletName: String,
        addressType: AddressType,
        totalSigns: Int,
        requireSigns: Int
    ) {
        if (groupId.isNotEmpty()) {
            viewModelScope.launch {
                updateGroupSandboxConfigUseCase(
                    UpdateGroupSandboxConfigUseCase.Params(
                        groupId = groupId,
                        name = walletName,
                        addressType = addressType,
                        m = requireSigns,
                        n = totalSigns
                    )
                ).onSuccess {
                    _state.update { it.copy(groupSandbox = it.groupSandbox?.copy(name = walletName)) }
                    _event.emit(AddWalletEvent.OnCreateWalletSuccess)
                }.onFailure {
                    _event.emit(AddWalletEvent.ShowError(it.message.orEmpty()))
                }
            }
        } else {
            viewModelScope.launch {
                _event.emit(AddWalletEvent.OnCreateWalletSuccess)
            }
        }
    }

    fun setMiniscriptLocal(miniscript: String) {
        viewModelScope.launch {
            setMiniscriptLocalUseCase(miniscript).onFailure { error ->
                _event.emit(AddWalletEvent.ShowError(error.message.orEmpty()))
            }
        }
    }

    fun clearMiniscriptLocal() {
        viewModelScope.launch {
            clearMiniscriptLocalUseCase(Unit).onFailure { error ->
                _event.emit(AddWalletEvent.ShowError(error.message.orEmpty()))
            }
        }
    }
}