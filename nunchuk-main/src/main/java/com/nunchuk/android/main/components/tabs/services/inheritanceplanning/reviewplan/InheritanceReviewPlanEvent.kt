package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan

sealed class InheritanceReviewPlanEvent {
    data class Loading(val loading: Boolean) : InheritanceReviewPlanEvent()
    data class CalculateRequiredSignaturesSuccess(
        val type: String,
        val walletId: String,
        val userData: String,
        val requiredSignatures: Int
    ) : InheritanceReviewPlanEvent()

    data class ProcessFailure(val message: String) : InheritanceReviewPlanEvent()
    object CreateOrUpdateInheritanceSuccess : InheritanceReviewPlanEvent()
    object CancelInheritanceSuccess : InheritanceReviewPlanEvent()
}

data class InheritanceReviewPlanState(
    val activationDate: Long = 0,
    val note: String = "",
    val isNotifyToday: Boolean = false,
    val emails: List<String> = emptyList(),
    val userData: String? = null,
    val walletId: String? = null,
    val walletName: String? = null,
    val isCreateOrUpdateFlow: Boolean = true
)