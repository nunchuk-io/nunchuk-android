package com.nunchuk.android.main.membership.plantype

import com.nunchuk.android.core.util.InheritancePlanType

sealed class InheritancePlanTypeEvent {
    data class OnContinueClicked(val selectedPlanType: InheritancePlanType) : InheritancePlanTypeEvent()
}

data class InheritancePlanTypeUiState(
    val selectedPlanType: InheritancePlanType = InheritancePlanType.OFF_CHAIN,
    val isPersonal: Boolean = false,
    val slug: String? = null,
    val walletType: String? = null,
    val changeTimelockFlow: Boolean? = null,
    val setupPreference: String? = null,
    val walletId: String? = null,
    val groupId: String? = null
)
