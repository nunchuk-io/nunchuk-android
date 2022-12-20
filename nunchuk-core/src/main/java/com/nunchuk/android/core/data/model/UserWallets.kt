package com.nunchuk.android.core.data.model

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.core.data.model.membership.KeyPoliciesDto
import com.nunchuk.android.core.data.model.membership.ServerKeyDto
import com.nunchuk.android.core.data.model.membership.TransactionServerDto

internal data class CreateServerKeysPayload(
    @SerializedName("policies")
    val keyPoliciesDtoPayload: KeyPoliciesDto? = null,
    @SerializedName("name")
    val name: String?,
    @SerializedName("wallet")
    val walletId: String? = null,
)

internal data class CreateSecurityQuestionRequest(
    @SerializedName("question")
    val question: String = ""
)

internal data class CreateSecurityQuestionResponse(
    @SerializedName("question")
    val question: SecurityQuestionResponse
)

internal data class CreateServerKeyResponse(
    @SerializedName("key") val key: ServerKeyDto? = null
)

internal data class UpdateWalletPayload(
    @SerializedName("name")
    val name: String? = null,
)

data class SecurityQuestionDataResponse(
    @SerializedName("questions")
    val questions: List<SecurityQuestionResponse>,
)

data class SecurityQuestionResponse(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("question")
    val question: String? = null,
    @SerializedName("is_answered")
    val isAnswer: Boolean? = null,
)

data class ConfigSecurityQuestionPayload(
    @SerializedName("questions_and_answers")
    val questionsAndAnswerRequests: List<QuestionsAndAnswerRequest>,
)

data class CalculateRequiredSignaturesSecurityQuestionPayload(
    @SerializedName("questions_and_answers")
    val questionsAndAnswerRequests: List<QuestionsAndAnswerRequest>,
    @SerializedName("wallet")
    val walletId: String
)

data class QuestionsAndAnswerRequest(
    @SerializedName("answer")
    val answer: String? = null,
    @SerializedName("question_id")
    val questionId: String = "",
)

data class SecurityQuestionsUpdateRequest(
    @SerializedName("nonce")
    val nonce: String? = null,
    @SerializedName("body")
    val body: QuestionsAndAnswerRequestBody? = null
)

data class QuestionsAndAnswerRequestBody(
    @SerializedName("questions_and_answers")
    val questionsAndAnswerRequests: List<QuestionsAndAnswerRequest>? = null,
    @SerializedName("wallet")
    val walletId: String? = null
)

data class LockdownUpdateRequest(
    @SerializedName("nonce")
    val nonce: String? = null,
    @SerializedName("body")
    val body: Body? = null
) {
    data class Body(
        @SerializedName("period_id")
        val periodId: String? = null,
        @SerializedName("wallet")
        val walletId: String? = null
    )
}

data class CreateUpdateInheritancePlanRequest(
    @SerializedName("nonce")
    val nonce: String? = null,
    @SerializedName("body")
    val body: Body? = null
) {
    data class Body(
        @SerializedName("note")
        val note: String? = null,
        @SerializedName("notification_emails")
        val notificationEmails: List<String>? = null,
        @SerializedName("notify_today")
        val notifyToday: Boolean? = null,
        @SerializedName("activation_time_milis")
        val activationTimeMilis: Long? = null,
        @SerializedName("wallet")
        val walletId: String? = null,
    )
}


data class InheritanceClaimStatusRequest(
    @SerializedName("nonce")
    val nonce: String? = null,
    @SerializedName("body")
    val body: Body? = null
) {
    data class Body(
        @SerializedName("magic")
        val magic: String? = null,
    )
}

data class InheritanceClaimDownloadBackupRequest(
    @SerializedName("magic")
    val magic: String? = null
)

data class InheritanceClaimCheckValidRequest(
    @SerializedName("magic")
    val magic: String? = null
)

data class InheritanceClaimCheckValidResponse(
    @SerializedName("is_valid")
    val isValid: Boolean? = null
)

data class InheritanceClaimClaimRequest(
    @SerializedName("magic")
    val magic: String? = null,
    @SerializedName("psbt")
    val psbt: String? = null
)

data class InheritanceCancelRequest(
    @SerializedName("nonce")
    val nonce: String? = null,
    @SerializedName("body")
    val body: Body? = null
) {
    data class Body(
        @SerializedName("wallet")
        val walletId: String? = null
    )
}

data class InheritanceClaimCreateTransactionRequest(
    @SerializedName("nonce")
    val nonce: String? = null,
    @SerializedName("body")
    val body: Body? = null
) {
    data class Body(
        @SerializedName("magic")
        val magic: String? = null,
        @SerializedName("address")
        val address: String? = null,
        @SerializedName("fee_rate")
        val feeRate: String? = null,
    )
}

data class InheritanceCheckRequest(
    @SerializedName("magic")
    val magic: String? = null,
    @SerializedName("environment")
    val environment: String? = null,
)

data class InheritanceCheckResponse(
    @SerializedName("is_valid")
    val isValid: Boolean? = null,
    @SerializedName("is_paid")
    val isPaid: Boolean? = null,
    @SerializedName("is_expired")
    val isExpired: Boolean? = null,
)

data class TransactionAdditionalResponse(
    @SerializedName("transaction") val transaction: TransactionServerDto? = null,
    @SerializedName("tx_fee") val txFee: Double? = null,
    @SerializedName("tx_fee_rate") val txFeeRate: Double? = null,
    @SerializedName("sub_amount") val subAmount: Double? = null
)

