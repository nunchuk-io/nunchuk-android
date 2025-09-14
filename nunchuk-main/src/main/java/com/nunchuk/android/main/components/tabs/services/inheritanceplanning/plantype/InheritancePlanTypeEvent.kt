package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.plantype

sealed class InheritancePlanTypeEvent {
    data class OnContinueClicked(val selectedPlanType: InheritancePlanType) : InheritancePlanTypeEvent()
}

enum class InheritancePlanType {
    OFF_CHAIN,
    ON_CHAIN
}

data class InheritancePlanTypeUiState(
    val selectedPlanType: InheritancePlanType = InheritancePlanType.OFF_CHAIN
)
