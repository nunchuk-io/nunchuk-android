package com.nunchuk.android.core.data.api

import com.nunchuk.android.core.data.model.*
import com.nunchuk.android.core.data.model.membership.*
import com.nunchuk.android.core.network.Data
import retrofit2.http.*

internal interface UserWalletsApi {
    @GET("/v1.1/user-wallets/server-keys/{key_id_or_xfp}")
    suspend fun getServerKey(@Path("key_id_or_xfp") keyIdOrXfp: String): Data<CreateServerKeyResponse>

    @POST("/v1.1/user-wallets/server-keys")
    suspend fun createServerKey(@Body payload: CreateServerKeysPayload): Data<CreateServerKeyResponse>

    @PUT("/v1.1/user-wallets/server-keys/{key_id_or_xfp}")
    suspend fun updateServerKeys(
        @HeaderMap headers: Map<String, String>,
        @Path("key_id_or_xfp") keyId: String,
        @Body body: KeyPolicyUpdateRequest
    ): Data<CreateServerKeyResponse>

    @GET("/v1.1/user-wallets/security-questions")
    suspend fun getSecurityQuestion(@Header("Verify-token") verifyToken: String?): Data<SecurityQuestionDataResponse>

    @POST("/v1.1/user-wallets/security-questions")
    suspend fun createSecurityQuestion(@Body request: CreateSecurityQuestionRequest): Data<CreateSecurityQuestionResponse>

    @PUT("/v1.1/user-wallets/security-questions")
    suspend fun configSecurityQuestion(@Body payload: ConfigSecurityQuestionPayload): Data<Unit>

    @POST("/v1.1/user-wallets/wallets")
    suspend fun createWallet(@Body payload: CreateWalletRequest): Data<CreateOrUpdateWalletResponse>

    @PUT("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}")
    suspend fun updateWallet(
        @Path("wallet_id_or_local_id") walletLocalId: String,
        @Body payload: UpdateWalletPayload
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
        @Body payload: CreateServerTransactionRequest
    ): Data<TransactionResponse>

    @POST("/v1.1/user-wallets/wallets/{wallet_id_or_local_id}/transactions/{transaction_id}/sign")
    suspend fun signServerTransaction(
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("transaction_id") transactionId: String,
        @Body payload: SignServerTransactionRequest
    ): Data<TransactionResponse>

    @GET("/v1.1/user-wallets/inheritance")
    suspend fun getInheritance(@Query("wallet") wallet: String) : Data<InheritanceResponse>

    @POST("/v1.1/user-wallets/user-keys/{key_id_or_xfp}/download-backup")
    suspend fun downloadBackup(
        @Header("Verify-token") verifyToken: String,
        @Path("key_id_or_xfp") id: String,
        @Body payload: ConfigSecurityQuestionPayload
    ): Data<com.nunchuk.android.model.KeyResponse>

    @POST("/v1.1/passport/verified-password-token/{target_action}")
    suspend fun verifiedPasswordToken(
        @Path("target_action") targetAction: String,
        @Body payload: VerifiedPasswordTokenRequest
    ): Data<VerifiedPasswordTokenResponse>

    @POST("/v1.1/user-wallets/security-questions/calculate-required-signatures")
    suspend fun calculateRequiredSignaturesSecurityQuestions(
        @Body payload: CalculateRequiredSignaturesSecurityQuestionPayload
    ) : Data<CalculateRequiredSignaturesResponse>

    @POST("/v1.1/user-wallets/server-keys/{key_id_or_xfp}/calculate-required-signatures")
    suspend fun calculateRequiredSignaturesUpdateServerKey(
        @Path("key_id_or_xfp") id: String,
        @Body payload: CreateServerKeysPayload
    ) : Data<CalculateRequiredSignaturesResponse>

    @PUT("/v1.1/user-wallets/security-questions/update")
    suspend fun securityQuestionsUpdate(
        @HeaderMap headers: Map<String, String>,
        @Body payload: SecurityQuestionsUpdateRequest
    )

    @GET("/v1.1/app/time/utc")
    suspend fun getCurrentServerTime(): Data<GetCurrentServerTimeResponse>
}