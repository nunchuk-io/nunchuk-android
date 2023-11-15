package com.nunchuk.android.main.membership.byzantine.selectrole

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.DefaultPermissions
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.toGroupWalletType
import com.nunchuk.android.usecase.membership.GetPermissionGroupWalletUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ByzantineSelectRoleViewModel @Inject constructor(
    val getPermissionGroupWalletUseCase: GetPermissionGroupWalletUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(AdvisorPlanSelectRoleState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<ByzantineSelectRoleEvent>()
    val event = _event.asSharedFlow()

    fun getPermissionGroupWallet(groupType: String) = viewModelScope.launch {
        if (state.value.roles.isNotEmpty()) return@launch
        val groupWalletType = groupType.toGroupWalletType() ?: return@launch
        _event.emit(ByzantineSelectRoleEvent.Loading(true))
        val result = getPermissionGroupWalletUseCase(groupWalletType)
        _event.emit(ByzantineSelectRoleEvent.Loading(false))
        if (result.isSuccess) {
            val permissions = result.getOrNull() ?: DefaultPermissions(emptyMap())
            val roles = mutableListOf<AdvisorPlanRoleOption>()
            permissions.permissions.forEach { (role, permission) ->
                if (permission.isNotEmpty() && role != AssistedWalletRole.MASTER.name) {
                    var desc = ""
                    permission.forEachIndexed { index, data ->
                        val name = data.alternativeNames[role] ?: data.name
                        desc += if (index == permission.size - 1) name
                        else "$name\n"
                    }
                    roles.add(AdvisorPlanRoleOption(role, desc))
                }
            }
            _state.update { it.copy(permissions = permissions, roles = roles) }
        } else {
            _event.emit(ByzantineSelectRoleEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun onOptionClick(role: String) {
        _state.update { it.copy(selectedRole = role) }
    }

    fun getSelectedRole(): String {
        return state.value.selectedRole
    }
}