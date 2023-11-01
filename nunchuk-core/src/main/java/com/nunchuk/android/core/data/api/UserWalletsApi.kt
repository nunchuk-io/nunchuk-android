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

package com.nunchuk.android.core.data.api

import com.nunchuk.android.core.data.model.*
import com.nunchuk.android.core.data.model.byzantine.*
import com.nunchuk.android.core.data.model.coin.CoinDataContent
import com.nunchuk.android.core.data.model.membership.*
import com.nunchuk.android.core.network.Data
import com.nunchuk.android.model.KeyResponse
import com.nunchuk.android.model.KeyResponseData
import com.nunchuk.android.model.KeyVerifiedRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

internal const val TRANSACTION_PAGE_COUNT: Int = 20

internal interface UserWalletsApi {
    @GET("/v1.1/user-wallets/server-keys/{key_id_or_xfp}")
    suspend fun getServerKey(
        @Path("key_id_or_xfp") keyIdOrXfp: String,
        @Query("derivation_path") derivationPath: String,
    ): Data<CreateServerKeyResponse>

    @POST("/v1.1/user-wallets/server-keys")
    suspend fun createServerKey(@Body payload: CreateServerKeysPayload): Data<CreateServerKeyResponse>

    @PUT("/v1.1/user-wallets/server-keys/{key_id_or_xfp}")
    suspend fun updateServerKeys(
        @HeaderMap headers: Map<String, String>,
        @Path("key_id_or_xfp") keyId: String,
        @Query("derivation_path") derivationPath: String,
        @Body body: KeyPolicyUpdateRequest
    ): Data<CreateServerKeyResponse>

    @GET("/v1.1/user-wallets/security-questions")
    suspend fun getSecurityQuestion(): Data<SecurityQuestionDataResponse>

    @POST("/v1.1/user-wallets/security-questions/verify-answer")
    suspend fun verifySecurityQuestion(@Body request: ConfigSecurityQuestionPayload): Data<VerifySecurityQuestionResponse>

    @POST("/v1.1/user-wallets/security-questions")
    suspend fun createSecurityQuestion(@Body request: CreateSecurityQuestionRequest): Data<CreateSecurityQuestionResponse>

    @PUT("/v1.1/user-wallets/security-questions")
    suspend fun configSecurityQuestion(@Body payload: ConfigSecurityQuestionPayload): Data<Unit>

    @POST("/v1.1/user-wallets/wallets")
    suspend fun createWallet(@Body payload: CreateWalletRequest): Data<CreateOrUpdateWalletResponse>

    @PUT("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}")
    suspend fun updateWallet(
        @Path("wallet_id_or_local_id") walletLocalId: String, @Body payload: UpdateWalletPayload
    ): Data<CreateOrUpdateWalletResponse>

    @GET("/v1.1/user-wallets/wallets")
    suspend fun getServerWallet(): Data<GetWalletResponse>

    @GET("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/transactions/{transaction_id}")
    suspend fun getTransaction(
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("transaction_id") transactionId: String,
    ): Data<TransactionResponse>

    @DELETE("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/transactions/{transaction_id}")
    suspend fun deleteTransaction(
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("transaction_id") transactionId: String,
    ): Data<TransactionResponse>

    @POST("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/transactions")
    suspend fun createTransaction(
        @Path("wallet_id_or_local_id") walletId: String,
        @Body payload: CreateOrUpdateServerTransactionRequest
    ): Data<TransactionResponse>

    @PUT("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/transactions/{transaction_id}")
    suspend fun updateTransaction(
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("transaction_id") transactionId: String,
        @Body payload: CreateOrUpdateServerTransactionRequest
    ): Data<TransactionResponse>

    @POST("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/transactions/{transaction_id}/sign")
    suspend fun signServerTransaction(
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("transaction_id") transactionId: String,
        @Body payload: SignServerTransactionRequest
    ): Data<TransactionResponse>

    @GET("/v1.1/user-wallets/inheritance")
    suspend fun getInheritance(
        @Query("wallet") wallet: String,
        @Query("group_id") groupId: String? = null,
    ): Data<InheritanceResponse>

    @POST("/v1.1/user-wallets/inheritance")
    suspend fun createInheritance(
        @HeaderMap headers: Map<String, String>,
        @Body payload: CreateUpdateInheritancePlanRequest,
        @Query("draft") draft: Boolean = false
    ): Data<InheritanceResponse>

    @PUT("/v1.1/user-wallets/inheritance")
    suspend fun updateInheritance(
        @HeaderMap headers: Map<String, String>,
        @Body payload: CreateUpdateInheritancePlanRequest,
        @Query("draft") draft: Boolean = false
    ): Data<InheritanceResponse>

    @POST("/v1.1/user-wallets/inheritance/calculate-required-signatures")
    suspend fun calculateRequiredSignaturesInheritance(
        @Body payload: CreateUpdateInheritancePlanRequest.Body
    ): Data<CalculateRequiredSignaturesResponse>

    @POST("/v1.1/user-wallets/user-keys/{key_id_or_xfp}/download-backup")
    suspend fun downloadBackup(
        @Header("Verify-token") verifyToken: String,
        @Path("key_id_or_xfp") id: String,
        @Body payload: ConfigSecurityQuestionPayload
    ): Data<KeyResponse>

    @GET("/v1.1/user-wallets/user-keys/{key_id_or_xfp}")
    suspend fun getKey(
        @Path("key_id_or_xfp") id: String,
        @Query("derivation_path") derivationPath: String,
    ): Data<KeyResponse>

    @POST("/v1.1/user-wallets/user-keys/{key_id_or_xfp}/calculate-required-signatures")
    suspend fun calculateRequiredSignaturesRecoverKey(
        @Path("key_id_or_xfp") id: String,
        @Body payload: EmptyRequest = EmptyRequest()
    ): Data<CalculateRequiredSignaturesResponse>

    @POST("/v1.1/user-wallets/user-keys/{key_id_or_xfp}/request-recover")
    suspend fun requestRecoverKey(
        @HeaderMap headers: Map<String, String>,
        @Path("key_id_or_xfp") id: String,
        @Body payload: RequestRecoverKeyRequest
    ): Data<TransactionAdditionalResponse>

    @POST("/v1.1/user-wallets/user-keys/{key_id_or_xfp}/recover")
    suspend fun recoverKey(
        @Path("key_id_or_xfp") id: String,
        @Body payload: EmptyRequest = EmptyRequest()
    ): Data<KeyResponseData>

    @POST("/v1.1/user-wallets/user-keys/{key_id_or_xfp}/mark-recover-status")
    suspend fun markRecoverStatus(
        @Path("key_id_or_xfp") id: String,
        @Body payload: MarkRecoverStatusRequest
    ): Data<Unit>

    @POST("/v1.1/user-wallets/security-questions/calculate-required-signatures")
    suspend fun calculateRequiredSignaturesSecurityQuestions(
        @Body payload: CalculateRequiredSignaturesSecurityQuestionPayload
    ): Data<CalculateRequiredSignaturesResponse>

    @POST("/v1.1/user-wallets/server-keys/{key_id_or_xfp}/calculate-required-signatures")
    suspend fun calculateRequiredSignaturesUpdateServerKey(
        @Path("key_id_or_xfp") id: String,
        @Query("derivation_path") derivationPath: String,
        @Body payload: CreateServerKeysPayload
    ): Data<CalculateRequiredSignaturesResponse>

    @PUT("/v1.1/user-wallets/security-questions/update")
    suspend fun securityQuestionsUpdate(
        @HeaderMap headers: Map<String, String>, @Body payload: SecurityQuestionsUpdateRequest
    )

    @GET("/v1.1/user-wallets/nonce")
    suspend fun getNonce(): Data<GetNonceResponse>

    @POST("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/transactions/{transaction_id}/schedule")
    suspend fun scheduleTransaction(
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("transaction_id") transactionId: String,
        @Body payload: ScheduleTransactionRequest,
    ): Data<TransactionResponse>

    @POST("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/transactions/{transaction_id}/sync")
    suspend fun syncTransaction(
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("transaction_id") transactionId: String,
        @Body payload: SyncTransactionRequest,
    ): Data<TransactionResponse>

    @DELETE("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/transactions/{transaction_id}/schedule")
    suspend fun deleteScheduleTransaction(
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("transaction_id") transactionId: String,
    ): Data<TransactionResponse>

    @GET("/v1.1/user-wallets/lockdown/period")
    suspend fun getLockdownPeriod(): Data<PeriodResponse>

    @POST("/v1.1/user-wallets/lockdown/lock")
    suspend fun lockdownUpdate(
        @HeaderMap headers: Map<String, String>, @Body payload: LockdownUpdateRequest
    ): Data<Unit>

    @POST("/v1.1/user-wallets/lockdown/calculate-required-signatures")
    suspend fun calculateRequiredSignaturesLockdown(
        @Body payload: LockdownUpdateRequest.Body
    ): Data<CalculateRequiredSignaturesResponse>

    @Multipart
    @POST("/v1.1/user-wallets/user-keys/upload-backup")
    suspend fun uploadBackupKey(
        @Part("key_name") keyName: RequestBody,
        @Part("key_type") keyType: RequestBody,
        @Part("key_xfp") keyXfp: RequestBody,
        @Part("card_id") cardId: RequestBody,
        @Part image: MultipartBody.Part,
    ): Data<KeyResponse>

    @POST("/v1.1/user-wallets/user-keys/{key_id}/verify")
    suspend fun setKeyVerified(
        @Path("key_id") keyId: String, @Body payload: KeyVerifiedRequest
    ): Data<Unit>

    @POST("/v1.1/user-wallets/inheritance/claiming/status")
    suspend fun inheritanceClaimingStatus(
        @HeaderMap headers: Map<String, String>, @Body payload: InheritanceClaimStatusRequest
    ): Data<InheritanceClaimStatusResponse>

    @POST("/v1.1/user-wallets/inheritance/claiming/download-backup")
    suspend fun inheritanceClaimingDownloadBackup(
        @Body payload: InheritanceClaimDownloadBackupRequest
    ): Data<KeyResponse>

    @POST("/v1.1/user-wallets/inheritance/claiming/create-transaction")
    suspend fun inheritanceClaimingCreateTransaction(
        @HeaderMap headers: Map<String, String>,
        @Body payload: InheritanceClaimCreateTransactionRequest
    ): Data<TransactionAdditionalResponse>

    @POST("/v1.1/user-wallets/inheritance/claiming/claim")
    suspend fun inheritanceClaimingClaim(
        @Body payload: InheritanceClaimClaimRequest
    ): Data<TransactionResponse>

    @POST("/v1.1/user-wallets/inheritance/claiming/check-valid")
    suspend fun inheritanceClaimingCheckValid(
        @Body payload: InheritanceClaimCheckValidRequest
    ): Data<InheritanceClaimCheckValidResponse>

    @HTTP(method = "DELETE", path = "/v1.1/user-wallets/inheritance", hasBody = true)
    suspend fun inheritanceCancel(
        @HeaderMap headers: Map<String, String>,
        @Body payload: InheritanceCancelRequest,
        @Query("draft") draft: Boolean = false
    ): Data<DummyTransactionResponse>

    @POST("/v1.1/user-wallets/inheritance/request-planning")
    suspend fun inheritanceRequestPlanning(
        @HeaderMap headers: Map<String, String>,
        @Body payload: InheritanceByzantineRequestPlanning,
        @Query("draft") draft: Boolean = false
    ): Data<DummyTransactionResponse>

    @POST("/v1.1/user-wallets/inheritance/check")
    suspend fun inheritanceCheck(
        @Body payload: InheritanceCheckRequest
    ): Data<InheritanceCheckResponse>

    @GET("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/transactions?limit=${TRANSACTION_PAGE_COUNT}&statuses=PENDING_SIGNATURES,READY_TO_BROADCAST&type=STANDARD,SCHEDULED,CLAIMING,ROLLOVER")
    suspend fun getTransactionsToSync(
        @Path("wallet_id_or_local_id") walletId: String, @Query("offset") offset: Int
    ): Data<TransactionsResponse>

    @GET("/v1.1/user-wallets/inheritance/buffer-period")
    suspend fun getInheritanceBufferPeriod(): Data<PeriodResponse>

    @POST("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/calculate-required-signatures")
    suspend fun calculateRequiredSignaturesDeleteAssistedWallet(
        @Path("wallet_id_or_local_id") walletId: String
    ): Data<CalculateRequiredSignaturesResponse>

    @HTTP(
        method = "DELETE",
        path = "/v1.1/user-wallets/wallets/{wallet_id_or_local_id}",
        hasBody = true
    )
    suspend fun deleteAssistedWallet(
        @Path("wallet_id_or_local_id") walletId: String,
        @HeaderMap headers: Map<String, String>,
        @Body payload: DeleteAssistedWalletRequest
    ): Data<TransactionResponse>

    @GET("/v1.1/user-wallets/configs")
    suspend fun getAssistedWalletConfig(): Data<AssistedWalletConfigResponse>

    @GET("/v1.1/group-wallets/configs")
    suspend fun getGroupAssistedWalletConfig(): Data<GroupAssistedWalletConfigResponse>

    @PUT("/v1.1/user-wallets/wallet-keys/{xfp}")
    suspend fun updateKeyName(
        @Path("xfp") xfp: String, @Body payload: UpdateKeyPayload
    ): Data<Unit>

    @POST("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/coin-control")
    suspend fun uploadCoinControlData(
        @Path("wallet_id_or_local_id") walletId: String, @Body payload: CoinDataContent
    ): Data<Unit>

    @GET("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/coin-control")
    suspend fun getCoinControlData(
        @Path("wallet_id_or_local_id") walletId: String,
    ): Data<CoinDataContent>

    @GET("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/transactions?limit=${TRANSACTION_PAGE_COUNT}&statuses=CANCELED&type=STANDARD,SCHEDULED,CLAIMING,ROLLOVER")
    suspend fun getTransactionsToDelete(
        @Path("wallet_id_or_local_id") walletId: String, @Query("offset") offset: Int
    ): Data<TransactionsResponse>

    @POST("/v1.1/user-wallets/draft-wallets/request-add-key")
    suspend fun requestAddKey(
        @Body payload: DesktopKeyRequest
    ): Data<RequestDesktopKeyResponse>

    @DELETE("/v1.1/user-wallets/draft-wallets/current")
    suspend fun deleteDraftWallet(): Data<Unit>

    @GET("/v1.1/user-wallets/draft-wallets/request-add-key/{request_id}")
    suspend fun getRequestAddKeyStatus(
        @Path("request_id") requestId: String,
    ): Data<RequestDesktopKeyResponse>

    @DELETE("/v1.1/user-wallets/draft-wallets/request-add-key/{request_id}")
    suspend fun cancelRequestAddKey(
        @Path("request_id") requestId: String,
    ): Data<Unit>

    @POST("/v1.1/user-wallets/draft-wallets/request-add-key/{request_id}/push")
    suspend fun pushRequestAddKey(
        @Path("request_id") requestId: String,
    ): Data<Unit>

    @POST("/v1.1/user-wallets/confirmation-code")
    suspend fun requestConfirmationCode(
        @Query("action") action: String,
        @Body payload: ConfirmationCodeRequest
    ): Data<ConfirmationCodeResponse>

    @POST("/v1.1/user-wallets/confirmation-code/{code_id}/verify")
    suspend fun verifyConfirmationCode(
        @Path("code_id") codeId: String,
        @Body payload: ConfirmationCodeVerifyRequest
    ): Data<ConfirmationCodeVerifyResponse>

    @DELETE("/v1.1/user-wallets/wallet-keys/{xfp}")
    suspend fun deleteKey(
        @Path("xfp") xfp: String,
    ): Data<Unit>

    @GET("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/transactions/notes?limit=${TRANSACTION_PAGE_COUNT}&statuses=CONFIRMED,NETWORK_REJECTED")
    suspend fun getConfirmedAndRejectedTransactions(
        @Path("wallet_id_or_local_id") walletId: String,
        @Query("offset") offset: Int
    ): Data<TransactionNoteResponse>
}