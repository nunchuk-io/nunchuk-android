package com.nunchuk.android.wallet.personal.components.add

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.usecase.GetFreeGroupWalletConfigUseCase
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
    private val getFreeGroupWalletConfigUseCase: GetFreeGroupWalletConfigUseCase,
    private val getGroupSandboxUseCase: GetGroupSandboxUseCase,
    private val updateGroupSandboxConfigUseCase: UpdateGroupSandboxConfigUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val groupId =
        savedStateHandle.get<String>(AddWalletActivity.GROUP_WALLET_ID).orEmpty()

    private val _state = MutableStateFlow(AddWalletState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<AddWalletEvent>()
    val event = _event.asSharedFlow()

    init {
        if (groupId.isNotEmpty()) {
            viewModelScope.launch {
                getGroupSandboxUseCase(groupId).onSuccess { group ->
                    val signers = group.signers.map {
                        it.takeIf { it.masterFingerprint.isNotEmpty() }?.toModel()
                    }
                    _state.update {
                        it.copy(
                            groupSandbox = group,
                            isHasSigner = signers.isNotEmpty()
                        )
                    }
                    getFreeGroupWalletConfig(group.addressType)
                }
            }
        }
    }

    fun getFreeGroupWalletConfig(addressType: AddressType) = viewModelScope.launch {
        viewModelScope.launch {
            getFreeGroupWalletConfigUseCase(addressType)
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