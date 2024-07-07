package com.nunchuk.android.main.membership.byzantine.selectrole

import com.nunchuk.android.model.DefaultPermissions
import com.nunchuk.android.model.byzantine.AssistedWalletRole

sealed class ByzantineSelectRoleEvent {
    data class Loading(val isLoading: Boolean) : ByzantineSelectRoleEvent()
    data class Error(val message: String) : ByzantineSelectRoleEvent()
    data object DowngradeInfo : ByzantineSelectRoleEvent()
}

data class AdvisorPlanSelectRoleState(
    val permissions: DefaultPermissions = DefaultPermissions(emptyMap()),
    val roles: List<AdvisorPlanRoleOption> = emptyList(),
    val selectedRole: String = AssistedWalletRole.NONE.name
)

data class AdvisorPlanRoleOption(val role: String, val desc: String)