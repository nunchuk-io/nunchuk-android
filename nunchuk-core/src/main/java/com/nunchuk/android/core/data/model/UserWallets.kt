package com.nunchuk.android.core.data.model

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.core.data.model.membership.KeyPoliciesDto
import com.nunchuk.android.core.data.model.membership.ServerKeyDto

internal data class CreateServerKeysPayload(
    @SerializedName("policies")
    val keyPoliciesDtoPayload: KeyPoliciesDto? = null,
    @SerializedName("name")
    val name: String,
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
    @SerializedName("key" ) val key : ServerKeyDto? = null
)

internal data class UpdateServerKeysPayload(
    @SerializedName("policies")
    val keyPoliciesDtoPayload: KeyPoliciesDto,
    @SerializedName("name")
    val name: String,
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
)

data class ConfigSecurityQuestionPayload(
    @SerializedName("questions_and_answers")
    val questionsAndAnswerRequests: List<QuestionsAndAnswerRequest>,
)

data class QuestionsAndAnswerRequest(
    @SerializedName("answer")
    val answer: String? = null,
    @SerializedName("question_id")
    val questionId: String = "",
)




