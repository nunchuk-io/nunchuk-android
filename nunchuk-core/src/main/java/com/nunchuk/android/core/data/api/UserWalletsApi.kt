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

import com.nunchuk.android.core.data.model.CalculateRequiredSignaturesSecurityQuestionPayload
import com.nunchuk.android.core.data.model.ChangeEmailRequest
import com.nunchuk.android.core.data.model.ChangeEmailSignatureRequest
import com.nunchuk.android.core.data.model.ConfigSecurityQuestionPayload
import com.nunchuk.android.core.data.model.CreateSecurityQuestionRequest
import com.nunchuk.android.core.data.model.CreateSecurityQuestionResponse
import com.nunchuk.android.core.data.model.CreateServerKeyResponse
import com.nunchuk.android.core.data.model.CreateServerKeysPayload
import com.nunchuk.android.core.data.model.CreateTimelockPayload
import com.nunchuk.android.core.data.model.CreateUpdateInheritancePlanRequest
import com.nunchuk.android.core.data.model.DeleteAssistedWalletRequest
import com.nunchuk.android.core.data.model.EmptyRequest
import com.nunchuk.android.core.data.model.InheritanceByzantineRequestPlanning
import com.nunchuk.android.core.data.model.InheritanceCancelRequest
import com.nunchuk.android.core.data.model.InheritanceCheckRequest
import com.nunchuk.android.core.data.model.InheritanceCheckResponse
import com.nunchuk.android.core.data.model.InheritanceClaimClaimRequest
import com.nunchuk.android.core.data.model.InheritanceClaimCreateTransactionRequest
import com.nunchuk.android.core.data.model.InheritanceClaimDownloadBackupRequest
import com.nunchuk.android.core.data.model.InheritanceClaimStatusRequest
import com.nunchuk.android.core.data.model.LockdownUpdateRequest
import com.nunchuk.android.core.data.model.MarkRecoverStatusRequest
import com.nunchuk.android.core.data.model.InitWalletConfigRequest
import com.nunchuk.android.core.data.model.RequestRecoverKeyRequest
import com.nunchuk.android.core.data.model.SecurityQuestionDataResponse
import com.nunchuk.android.core.data.model.SecurityQuestionsUpdateRequest
import com.nunchuk.android.core.data.model.SupportedSignersData
import com.nunchuk.android.core.data.model.SyncTransactionRequest
import com.nunchuk.android.core.data.model.TransactionAdditionalResponse
import com.nunchuk.android.core.data.model.UpdateKeyPayload
import com.nunchuk.android.core.data.model.UpdateSecurityQuestionResponse
import com.nunchuk.android.core.data.model.UpdateWalletPayload
import com.nunchuk.android.core.data.model.UserWalletConfigsSetupResponse
import com.nunchuk.android.core.data.model.byzantine.CreateDraftWalletRequest
import com.nunchuk.android.core.data.model.byzantine.DraftWalletResponse
import com.nunchuk.android.core.data.model.byzantine.DummyTransactionResponse
import com.nunchuk.android.core.data.model.byzantine.GroupAlertResponse
import com.nunchuk.android.core.data.model.byzantine.HealthCheckRequest
import com.nunchuk.android.core.data.model.byzantine.SavedAddressListResponse
import com.nunchuk.android.core.data.model.byzantine.SavedAddressRequest
import com.nunchuk.android.core.data.model.byzantine.SignInDummyTransactionResponse
import com.nunchuk.android.core.data.model.byzantine.TotalAlertResponse
import com.nunchuk.android.core.data.model.byzantine.WalletHealthStatusResponse
import com.nunchuk.android.core.data.model.coin.CoinDataContent
import com.nunchuk.android.core.data.model.membership.BatchTransactionPayload
import com.nunchuk.android.core.data.model.membership.CalculateRequiredSignaturesResponse
import com.nunchuk.android.core.data.model.membership.ConfirmationCodeRequest
import com.nunchuk.android.core.data.model.membership.ConfirmationCodeResponse
import com.nunchuk.android.core.data.model.membership.ConfirmationCodeVerifyRequest
import com.nunchuk.android.core.data.model.membership.ConfirmationCodeVerifyResponse
import com.nunchuk.android.core.data.model.membership.CreateOrUpdateServerTransactionRequest
import com.nunchuk.android.core.data.model.membership.CreateOrUpdateWalletResponse
import com.nunchuk.android.core.data.model.membership.CreateWalletRequest
import com.nunchuk.android.core.data.model.membership.DesktopKeyRequest
import com.nunchuk.android.core.data.model.membership.GetNonceResponse
import com.nunchuk.android.core.data.model.membership.GetWalletResponse
import com.nunchuk.android.core.data.model.membership.GroupAssistedWalletConfigResponse
import com.nunchuk.android.core.data.model.membership.HealthCheckHistoryResponseData
import com.nunchuk.android.core.data.model.membership.HealthReminderRequest
import com.nunchuk.android.core.data.model.membership.HealthReminderResponse
import com.nunchuk.android.core.data.model.membership.InheritanceClaimStatusResponse
import com.nunchuk.android.core.data.model.membership.InheritanceResponse
import com.nunchuk.android.core.data.model.membership.KeyPolicyUpdateRequest
import com.nunchuk.android.core.data.model.membership.PeriodResponse
import com.nunchuk.android.core.data.model.membership.RandomizeBroadcastBatchTransactionsPayload
import com.nunchuk.android.core.data.model.membership.RequestDesktopKeyResponse
import com.nunchuk.android.core.data.model.membership.ScheduleTransactionRequest
import com.nunchuk.android.core.data.model.membership.SignServerTransactionRequest
import com.nunchuk.android.core.data.model.membership.SignerServerDto
import com.nunchuk.android.core.data.model.membership.SigninDummyRequest
import com.nunchuk.android.core.data.model.membership.TransactionNoteResponse
import com.nunchuk.android.core.data.model.membership.TransactionResponse
import com.nunchuk.android.core.data.model.membership.TransactionsResponse
import com.nunchuk.android.core.data.model.membership.VerifySecurityQuestionResponse
import com.nunchuk.android.core.data.model.replacement.WalletReplacementStatusResponse
import com.nunchuk.android.core.network.Data
import com.nunchuk.android.model.DownloadBackupKeyResponseData
import com.nunchuk.android.model.KeyResponse
import com.nunchuk.android.model.KeyResponseData
import com.nunchuk.android.model.KeyVerifiedRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.HeaderMap
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

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

    @GET("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}")
    suspend fun getWallet(
        @Path("wallet_id_or_local_id") walletLocalId: String
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

    @POST("/v1.1/user-wallets/draft-wallets/{xfp}/download-backup")
    suspend fun downloadBackup(
        @Header("Verify-token") verifyToken: String? = null,
        @Path("xfp") xfp: String,
        @Body payload: ConfigSecurityQuestionPayload = ConfigSecurityQuestionPayload(emptyList())
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
        @HeaderMap headers: Map<String, String>,
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
        @HeaderMap headers: Map<String, String>, @Body payload: SecurityQuestionsUpdateRequest,
        @Query("draft") draft: Boolean = false
    ): Data<UpdateSecurityQuestionResponse>

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

    @POST("/v1.1/user-wallets/email-change/calculate-required-signatures")
    suspend fun calculateRequiredSignaturesChangeEmail(
        @Body payload: ChangeEmailSignatureRequest
    ): Data<CalculateRequiredSignaturesResponse>

    @POST("/v1.1/user-wallets/email-change/change")
    suspend fun changeEmail(
        @HeaderMap headers: Map<String, String>,
        @Body payload: ChangeEmailRequest,
        @Query("draft") draft: Boolean = false
    ): Data<DummyTransactionResponse>

    @Multipart
    @POST("/v1.1/user-wallets/draft-wallets/upload-backup")
    suspend fun uploadBackupKey(
        @Part("key_name") keyName: RequestBody,
        @Part("key_type") keyType: RequestBody,
        @Part("key_xfp") keyXfp: RequestBody,
        @Part("card_id") cardId: RequestBody,
        @Part image: MultipartBody.Part,
    ): Data<KeyResponse>

    @POST("/v1.1/user-wallets/draft-wallets/{xfp}/verify")
    suspend fun setKeyVerified(
        @Path("xfp") xfp: String, @Body payload: KeyVerifiedRequest
    ): Data<Unit>

    @POST("/v1.1/user-wallets/inheritance/claiming/status")
    suspend fun inheritanceClaimingStatus(
        @HeaderMap headers: Map<String, String>, @Body payload: InheritanceClaimStatusRequest
    ): Data<InheritanceClaimStatusResponse>

    @POST("/v1.1/user-wallets/inheritance/claiming/download-backups")
    suspend fun inheritanceClaimingDownloadBackups(
        @Body payload: InheritanceClaimDownloadBackupRequest
    ): Data<DownloadBackupKeyResponseData>

    @POST("/v1.1/user-wallets/inheritance/claiming/create-transaction")
    suspend fun inheritanceClaimingCreateTransaction(
        @HeaderMap headers: Map<String, String>,
        @Body payload: InheritanceClaimCreateTransactionRequest
    ): Data<TransactionAdditionalResponse>

    @POST("/v1.1/user-wallets/inheritance/claiming/claim")
    suspend fun inheritanceClaimingClaim(
        @Body payload: InheritanceClaimClaimRequest
    ): Data<TransactionResponse>

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

    @GET("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/transactions?limit=${TRANSACTION_PAGE_COUNT}&statuses=PENDING_SIGNATURES,READY_TO_BROADCAST&types=STANDARD,SCHEDULED,CLAIMING,ROLLOVER,RECURRING")
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

    @GET("/v1.1/group-wallets/configs")
    suspend fun getGroupAssistedWalletConfig(): Data<GroupAssistedWalletConfigResponse>

    @PUT("/v1.1/user-wallets/wallet-keys/{xfp}")
    suspend fun updateServerKey(
        @Path("xfp") xfp: String,
        @Body payload: UpdateKeyPayload,
        @Query("derivation_path") derivationPath: String
    ): Data<Unit>

    @POST("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/coin-control")
    suspend fun uploadCoinControlData(
        @Path("wallet_id_or_local_id") walletId: String, @Body payload: CoinDataContent
    ): Data<Unit>

    @GET("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/coin-control")
    suspend fun getCoinControlData(
        @Path("wallet_id_or_local_id") walletId: String,
    ): Data<CoinDataContent>

    @GET("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/transactions?limit=${TRANSACTION_PAGE_COUNT}&statuses=CANCELED&types=STANDARD,SCHEDULED,CLAIMING,ROLLOVER,RECURRING")
    suspend fun getTransactionsToDelete(
        @Path("wallet_id_or_local_id") walletId: String, @Query("offset") offset: Int
    ): Data<TransactionsResponse>

    @POST("/v1.1/user-wallets/draft-wallets/request-add-key")
    suspend fun requestAddKey(
        @Body payload: DesktopKeyRequest
    ): Data<RequestDesktopKeyResponse>

    @POST("/v1.1/user-wallets/draft-wallets/add-key")
    suspend fun addKeyToServer(
        @Body payload: SignerServerDto,
    ): Data<SignerServerDto>

    @POST("/v1.1/user-wallets/draft-wallets/set-server-key")
    suspend fun setServerKey(
        @Body payload: Map<String, String>,
    ): Data<CreateServerKeyResponse>

    @POST("/v1.1/user-wallets/draft-wallets/init")
    suspend fun initDraftWallet(
        @Body config: InitWalletConfigRequest
    ): Data<DraftWalletResponse>

    @GET("/v1.1/user-wallets/draft-wallets/current")
    suspend fun getDraftWallet(): Data<DraftWalletResponse>

    @DELETE("/v1.1/user-wallets/draft-wallets/current")
    suspend fun deleteDraftWallet(): Data<Unit>

    @PUT("/v1.1/user-wallets/draft-wallets/timelock")
    suspend fun createDraftWalletTimelock(
        @Body payload: CreateTimelockPayload
    ): Data<DraftWalletResponse>

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

    @GET("/v1.1/user-wallets/wallet-keys/{xfp}/history")
    suspend fun healthCheckHistory(
        @Path("xfp") xfp: String,
    ): Data<HealthCheckHistoryResponseData>

    @GET("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/transactions/notes?limit=${TRANSACTION_PAGE_COUNT}&statuses=CONFIRMED,NETWORK_REJECTED")
    suspend fun getConfirmedAndRejectedTransactions(
        @Path("wallet_id_or_local_id") walletId: String,
        @Query("offset") offset: Int
    ): Data<TransactionNoteResponse>

    @PUT("/v1.1/user-wallets/inheritance/request-planning/{request_id}/deny")
    suspend fun denyInheritanceRequestPlanning(
        @Path("request_id") requestId: String, @QueryMap query: Map<String, String>
    ): Data<Unit>

    @PUT("/v1.1/user-wallets/inheritance/request-planning/{request_id}/approve")
    suspend fun approveInheritanceRequestPlanning(
        @Path("request_id") requestId: String, @QueryMap query: Map<String, String>
    ): Data<Unit>

    @PUT("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/transactions/{transaction_id}/rbf")
    suspend fun replaceTransaction(
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("transaction_id") transactionId: String,
        @Body payload: CreateOrUpdateServerTransactionRequest
    ): Data<TransactionResponse>

    @GET("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/alerts")
    suspend fun getAlerts(
        @Path("wallet_id_or_local_id") walletId: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int = TRANSACTION_PAGE_COUNT,
    ): Data<GroupAlertResponse>

    @PUT("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/alerts/{alert_id}/mark-as-read")
    suspend fun markAlertAsRead(
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("alert_id") alertId: String,
    ): Data<Unit>

    @PUT("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/alerts/{alert_id}/dismiss")
    suspend fun dismissAlert(
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("alert_id") alertId: String,
    ): Data<Unit>

    @GET("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/alerts/total")
    suspend fun getAlertTotal(
        @Path("wallet_id_or_local_id") walletId: String,
    ): Data<TotalAlertResponse>

    @GET("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/health")
    suspend fun getWalletHealthStatus(
        @Path("wallet_id_or_local_id") walletId: String,
    ): Data<WalletHealthStatusResponse>

    @POST("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/health/{xfp}/health-check")
    suspend fun healthCheck(
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("xfp") xfp: String,
        @Query("draft") draft: Boolean,
        @Body request: HealthCheckRequest,
    ): Data<DummyTransactionResponse>

    @GET("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/dummy-transactions/{dummy_transaction_id}")
    suspend fun getDummyTransaction(
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("dummy_transaction_id") transactionId: String,
    ): Data<DummyTransactionResponse>

    @PUT("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/dummy-transactions/{dummy_transaction_id}")
    suspend fun updateDummyTransaction(
        @HeaderMap headers: Map<String, String>,
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("dummy_transaction_id") transactionId: String,
    ): Data<DummyTransactionResponse>

    @PUT("/v1.1/user-wallets/signin-dummy/{dummy_transaction_id}")
    suspend fun updateDummyTransactionSignIn(
        @HeaderMap headers: Map<String, String>,
        @Path("dummy_transaction_id") transactionId: String,
    ): Data<SignInDummyTransactionResponse>

    @DELETE("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/dummy-transactions/{dummy_transaction_id}")
    suspend fun deleteDummyTransaction(
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("dummy_transaction_id") transactionId: String,
    ): Data<Unit>

    @PUT("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/dummy-transactions/{dummy_transaction_id}/finalize")
    suspend fun finalizeDummyTransaction(
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("dummy_transaction_id") transactionId: String,
    ): Data<DummyTransactionResponse>

    @GET("/v1.1/user-wallets/saved-address")
    suspend fun getSavedAddressList(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int = TRANSACTION_PAGE_COUNT,
    ): Data<SavedAddressListResponse>

    @PUT("/v1.1/user-wallets/saved-address")
    suspend fun addOrUpdateSavedAddress(
        @Body payload: SavedAddressRequest
    ): Data<SavedAddressListResponse>

    @DELETE("/v1.1/user-wallets/saved-address/{address}")
    suspend fun deleteSavedAddress(
        @Path("address") address: String
    ): Data<Unit>

    @GET("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/health/reminder")
    suspend fun getHealthReminders(
        @Path("wallet_id_or_local_id") walletId: String,
    ): Data<HealthReminderResponse>

    @PUT("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/health/reminder")
    suspend fun addOrUpdateHealthReminder(
        @Path("wallet_id_or_local_id") walletId: String,
        @Body payload: HealthReminderRequest,
    ): Data<Unit>

    @DELETE("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/health/reminder")
    suspend fun deleteHealthReminder(
        @Path("wallet_id_or_local_id") walletId: String,
        @Query("xfps") xfps: List<String>,
    ): Data<Unit>

    @DELETE("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/health/reminder/{xfp}")
    suspend fun skipHealthReminder(
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("xfp") xfp: String,
    ): Data<Unit>

    @POST("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/replacement/{xfp}")
    suspend fun initReplaceKey(
        @Header("Verify-token") verifyToken: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("xfp") xfp: String,
    ): Data<Unit>

    @DELETE("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/replacement/{xfp}")
    suspend fun cancelReplaceKey(
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("xfp") xfp: String,
    ): Data<Unit>

    @POST("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/replacement/{xfp}/replace")
    suspend fun replaceKey(
        @Header("Verify-token") verifyToken: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("xfp") xfp: String,
        @Body payload: SignerServerDto
    ): Data<Unit>

    @POST("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/replacement/finalize")
    suspend fun finalizeReplaceWallet(
        @Header("Verify-token") verifyToken: String,
        @Path("wallet_id_or_local_id") walletId: String,
    ): Data<CreateOrUpdateWalletResponse>

    @GET("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/replacement/status")
    suspend fun getReplaceWalletStatus(
        @Path("wallet_id_or_local_id") walletId: String,
    ): Data<WalletReplacementStatusResponse>

    @DELETE("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/replacement/reset")
    suspend fun resetReplaceWallet(
        @Header("Verify-token") verifyToken: String,
        @Path("wallet_id_or_local_id") walletId: String,
    ): Data<Unit>

    @PUT("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/replacement/configs")
    suspend fun updateReplaceWalletConfigs(
        @Path("wallet_id_or_local_id") walletId: String,
        @Body payload: Map<String, Boolean>,
    ): Data<Unit>

    @POST("/v1.1/user-wallets/signin-dummy")
    suspend fun signinDummy(
        @Body payload: SigninDummyRequest,
    ): Data<SignInDummyTransactionResponse>

    @POST("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/transactions/batch")
    suspend fun batchTransactions(
        @Path("wallet_id_or_local_id") walletId: String,
        @Body payload: BatchTransactionPayload
    ): Data<TransactionsResponse>

    @POST("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/transactions/batch/randomize-broadcast")
    suspend fun randomizeBroadcastBatchTransactions(
        @Path("wallet_id_or_local_id") walletId: String,
        @Body payload: RandomizeBroadcastBatchTransactionsPayload
    ): Data<Unit>

    @POST("/v1.1/user-wallets/wallets/create-from-draft")
    suspend fun createWalletFromDraft(
        @Body payload: CreateDraftWalletRequest
    ): Data<CreateOrUpdateWalletResponse>

    @GET("/v1.1/user-wallets/draft-wallets/current/alerts")
    suspend fun getDraftWalletAlerts(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int = TRANSACTION_PAGE_COUNT,
    ): Data<GroupAlertResponse>

    @PUT("/v1.1/user-wallets/draft-wallets/current/alerts/{alert_id}/dismiss")
    suspend fun dismissDraftWalletAlert(
        @Path("alert_id") alertId: String,
    ): Data<Unit>

    @GET("/v1.1/user-wallets/draft-wallets/current/alerts/total")
    suspend fun getDraftWalletAlertTotal(): Data<TotalAlertResponse>

    @PUT("/v1.1/user-wallets/draft-wallets/current/alerts/{alert_id}/mark-as-read")
    suspend fun markDraftWalletAlertAsRead(
        @Path("alert_id") alertId: String,
    ): Data<Unit>

    @Multipart
    @POST("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/replacement/upload-backup")
    suspend fun uploadBackupKeyReplacement(
        @Header("Verify-token") verifyToken: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Part("key_name") keyName: RequestBody,
        @Part("key_type") keyType: RequestBody,
        @Part("key_xfp") keyXfp: RequestBody,
        @Part("card_id") cardId: RequestBody,
        @Part image: MultipartBody.Part,
    ): Data<KeyResponse>

    @POST("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/replacement/{xfp}/download-backup")
    suspend fun downloadBackupReplacement(
        @Header("Verify-token") verifyToken: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("xfp") xfp: String,
    ): Data<KeyResponse>

    @POST("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/replacement/{xfp}/verify")
    suspend fun setKeyVerifiedReplacement(
        @Header("Verify-token") verifyToken: String,
        @Path("xfp") keyId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Body payload: KeyVerifiedRequest
    ): Data<Unit>

    @GET("/v1.1/user-wallets/taproot/supported-signers")
    suspend fun getSupportedSigners(): Data<SupportedSignersData>

    @DELETE("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/replacement/{xfp}/remove")
    suspend fun removeKeyReplacement(
        @Header("Verify-token") verifyToken: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("xfp") xfp: String,
    ): Data<Unit>

    @GET("/v1.1/user-wallets/configs/setup")
    suspend fun getUserWalletConfigsSetup(): Data<UserWalletConfigsSetupResponse>
}