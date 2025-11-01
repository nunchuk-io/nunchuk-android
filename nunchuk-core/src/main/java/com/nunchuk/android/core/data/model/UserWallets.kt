/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
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
import com.nunchuk.android.core.data.model.byzantine.DummyTransactionDto
import com.nunchuk.android.core.data.model.byzantine.WalletConfigDto
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

internal data class CreateTimelockPayload(
    @SerializedName("timelock")
    val timelock: TimelockPayload
)

internal data class TimelockPayload(
    @SerializedName("value")
    val value: Long
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
    @SerializedName("key") val key: ServerKeyDto? = null,
    @SerializedName("dummy_transaction") val dummyTransaction: DummyTransactionDto? = null
)

internal data class UpdateWalletPayload(
    @SerializedName("name")
    val name: String? = null,
)

internal data class UpdateKeyPayload(
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("tags")
    val tags: List<String>?=null,
    @SerializedName("tapsigner")
    val tapSignerPayload: TapSignerPayload? = null,
)

internal data class TapSignerPayload(
    @SerializedName("card_id")
    val cardId: String? = null,
    @SerializedName("version")
    val version: String? = null,
    @SerializedName("birth_height")
    val birthHeight: Int? = null,
    @SerializedName("is_testnet")
    val isTestNet: Boolean? = null,
    @SerializedName("is_inheritance")
    val isInheritance: Boolean? = null,
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
    val body: Body? = null
) {
    data class Body(
        @SerializedName("questions_and_answers")
        val questionsAndAnswerRequests: List<QuestionsAndAnswerRequest>? = null,
        @SerializedName("wallet")
        val walletId: String? = null
    )
}

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
        val walletId: String? = null,
        @SerializedName("group_id")
        val groupId: String? = null,
    )
}

data class ChangeEmailSignatureRequest(
    @SerializedName("new_email")
    val newEmail: String? = null
)

data class ChangeEmailRequest(
    @SerializedName("nonce")
    val nonce: String? = null,
    @SerializedName("body")
    val body: Body? = null
) {
    data class Body(
        @SerializedName("new_email")
        val newEmail: String? = null
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
        val magic: String? = null
    )
}

data class InheritanceClaimDownloadBackupRequest(
    @SerializedName("magic")
    val magic: String? = null,
    @SerializedName("hashed_bps")
    val hashedBps: List<String>? = null
)

data class InheritanceClaimClaimRequest(
    @SerializedName("magic")
    val magic: String? = null,
    @SerializedName("psbt")
    val psbt: String? = null
)

data class InheritanceClaimingInitRequest(
    @SerializedName("magic")
    val magic: String? = null
)

data class InheritanceCancelRequest(
    @SerializedName("nonce")
    val nonce: String? = null,
    @SerializedName("body")
    val body: Body? = null
) {
    data class Body(
        @SerializedName("wallet")
        val walletId: String? = null,
        @SerializedName("group_id")
        val groupId: String? = null
    )
}

data class InheritanceByzantineRequestPlanning(
    @SerializedName("nonce")
    val nonce: String? = null,
    @SerializedName("body")
    val body: Body? = null
) {
    data class Body(
        @SerializedName("wallet")
        val walletId: String? = null,
        @SerializedName("group_id")
        val groupId: String? = null
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
        @SerializedName("amount")
        val amount: String? = null,
        @SerializedName("anti_fee_sniping")
        val antiFeeSniping: Boolean? = null,
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
    @SerializedName("fee") val txFee: Double? = null,
    @SerializedName("fee_rate") val txFeeRate: Double? = null,
    @SerializedName("sub_amount") val subAmount: Double? = null,
    @SerializedName("change_pos") val changePos: Int? = null,
)

data class DeleteAssistedWalletRequest(
    @SerializedName("nonce")
    val nonce: String? = null
)

data class RequestRecoverKeyRequest(
    @SerializedName("nonce")
    val nonce: String? = null,
    @SerializedName("body")
    val body: EmptyRequest = EmptyRequest()
)

data class MarkRecoverStatusRequest(
    @SerializedName("status")
    val status: String? = null
)

class EmptyRequest

data class UpdateSecurityQuestionResponse(
    @SerializedName("dummy_transaction")
    val dummyTransaction: DummyTransactionDto? = null
)

internal data class InitWalletConfigRequest(
    @SerializedName("wallet_config")
    val walletConfig: WalletConfigDto? = null,
    @SerializedName("wallet_type")
    val walletType: String? = null
)
