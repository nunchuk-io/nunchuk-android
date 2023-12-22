package com.nunchuk.android.transaction.components.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.ByzantineMember
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RequestSignatureMemberViewModel @Inject constructor(
    private val getGroupUseCase: GetGroupUseCase,
    private val assistedWalletManager: AssistedWalletManager
) : ViewModel() {

    private val _state = MutableStateFlow(RequestSignatureMemberState())
    val state = _state.asStateFlow()


    fun init(walletId: String) {
        viewModelScope.launch {
            getGroupUseCase(
                GetGroupUseCase.Params(
                    assistedWalletManager.getGroupId(walletId).orEmpty()
                )
            )
                .map { it.getOrElse { null } }
                .distinctUntilChanged()
                .collect { group ->
                    val members = group?.members.orEmpty()
                        .filter { it.role != AssistedWalletRole.OBSERVER.name && it.role != AssistedWalletRole.MASTER.name && it.isPendingRequest().not()}
                    _state.update { it.copy(members = members) }
                }
        }
    }
}

data class RequestSignatureMemberState(
    val members: List<ByzantineMember> = emptyList()
)