package com.nunchuk.android.main.membership.plantype

import com.nunchuk.android.core.util.InheritancePlanType

sealed class InheritancePlanTypeEvent {
    data class OnContinueClicked(val selectedPlanType: InheritancePlanType) : InheritancePlanTypeEvent()
}

data class InheritancePlanTypeUiState(
    val selectedPlanType: InheritancePlanType? = null,
    val isPersonal: Boolean = false,
    val slug: String? = null,
    val walletType: String? = null,
    val changeTimelockFlow: Int = -1,
    val setupPreference: String? = null,
    val walletId: String? = null,
    val groupId: String? = null,
    val orderedPlanTypes: List<InheritancePlanType> = listOf(InheritancePlanType.OFF_CHAIN, InheritancePlanType.ON_CHAIN)
)
