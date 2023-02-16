/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

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
    @SerializedName("change")
    val change: Boolean? = null
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
        @SerializedName("buffer_period_id")
        val bufferPeriodId: String? = null
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

