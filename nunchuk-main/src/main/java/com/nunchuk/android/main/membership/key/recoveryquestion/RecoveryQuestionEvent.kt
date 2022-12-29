package com.nunchuk.android.main.membership.key.recoveryquestion

import com.nunchuk.android.main.membership.model.SecurityQuestionModel

sealed class RecoveryQuestionEvent {
    data class Loading(val isLoading: Boolean) : RecoveryQuestionEvent()
    object ContinueStepEvent : RecoveryQuestionEvent()
    object ConfigRecoveryQuestionSuccess : RecoveryQuestionEvent()

    data class GetSecurityQuestionSuccess(
        val questions: List<SecurityQuestionModel>,
    ) : RecoveryQuestionEvent()

    data class CalculateRequiredSignaturesSuccess(
        val walletId: String,
        val userData: String,
        val requiredSignatures: Int,
        val type: String,
    ) : RecoveryQuestionEvent()

    data class ShowError(val message: String) : RecoveryQuestionEvent()
    object RecoveryQuestionUpdateSuccess : RecoveryQuestionEvent()
    object DiscardChangeClick : RecoveryQuestionEvent()
}

data class RecoveryQuestionState(
    val recoveries: List<RecoveryData> = emptyList(),
    val securityQuestions: List<SecurityQuestionModel> = emptyList(),
    val interactQuestionIndex: Int = InitValue,
    val userData: String? = null,
    val clearFocusRequest: Boolean = false
) {
    companion object {
        val Empty = RecoveryQuestionState()
        const val InitValue = -1
    }
}

data class RecoveryData(
    val index: Int,
    val question: SecurityQuestionModel = SecurityQuestionModel(),
    val answer: String = "",
    val change: Boolean = false,
    val isShowMask: Boolean = false
)