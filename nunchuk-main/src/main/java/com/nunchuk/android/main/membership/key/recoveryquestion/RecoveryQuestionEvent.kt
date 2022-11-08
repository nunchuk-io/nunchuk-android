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
        val requiredSignatures: Int
    ) : RecoveryQuestionEvent()

    data class ShowError(val message: String) : RecoveryQuestionEvent()
    object RecoveryQuestionUpdateSuccess : RecoveryQuestionEvent()
}

data class RecoveryQuestionState(
    val recoveries: List<RecoveryData> = recoveryListInitialize(),
    val securityQuestions: List<SecurityQuestionModel> = emptyList(),
    val interactQuestionIndex: Int = InitValue,
    val userData: String? = null
) {
    companion object {
        val Empty = RecoveryQuestionState()
        const val InitValue = -1
    }
}

private fun recoveryListInitialize(): List<RecoveryData> {
    val recoveryList = mutableListOf<RecoveryData>()
    (0..2).forEach {
        recoveryList.add(RecoveryData(index = it))
    }
    return recoveryList
}

data class RecoveryData(
    val index: Int,
    val question: SecurityQuestionModel = SecurityQuestionModel(),
    val answer: String = "",
)