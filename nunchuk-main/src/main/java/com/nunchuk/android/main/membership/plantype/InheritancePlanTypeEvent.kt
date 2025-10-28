package com.nunchuk.android.main.membership.plantype

sealed class InheritancePlanTypeEvent {
    data class OnContinueClicked(val selectedPlanType: InheritancePlanType) : InheritancePlanTypeEvent()
}

enum class InheritancePlanType {
    OFF_CHAIN,
    ON_CHAIN
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
