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
        @Path("key_id_or_xfp") keyId: String,
        @Body payload: UpdateServerKeysPayload
    ): Data<CreateServerKeyResponse>

    @GET("/v1.1/user-wallets/security-questions")
    suspend fun getSecurityQuestion(): Data<SecurityQuestionDataResponse>

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
}