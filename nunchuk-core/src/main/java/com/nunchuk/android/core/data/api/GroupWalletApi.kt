package com.nunchuk.android.core.data.api

import com.nunchuk.android.core.data.model.CreateServerKeyResponse
import com.nunchuk.android.core.data.model.CreateServerKeysPayload
import com.nunchuk.android.core.data.model.CreateTimelockPayload
import com.nunchuk.android.core.data.model.DeleteAssistedWalletRequest
import com.nunchuk.android.core.data.model.LockdownUpdateRequest
import com.nunchuk.android.core.data.model.SyncTransactionRequest
import com.nunchuk.android.core.data.model.UpdateWalletPayload
import com.nunchuk.android.core.data.model.byzantine.CreateDraftWalletRequest
import com.nunchuk.android.core.data.model.byzantine.CreateGroupRequest
import com.nunchuk.android.core.data.model.byzantine.CreateOrUpdateGroupChatRequest
import com.nunchuk.android.core.data.model.byzantine.DeletedGroupWalletsResponse
import com.nunchuk.android.core.data.model.byzantine.DraftWalletResponse
import com.nunchuk.android.core.data.model.byzantine.DummyTransactionResponse
import com.nunchuk.android.core.data.model.byzantine.EditGroupMemberRequest
import com.nunchuk.android.core.data.model.byzantine.GetGroupsResponse
import com.nunchuk.android.core.data.model.byzantine.GroupAlertResponse
import com.nunchuk.android.core.data.model.byzantine.GroupChatDataResponse
import com.nunchuk.android.core.data.model.byzantine.GroupChatListDataResponse
import com.nunchuk.android.core.data.model.byzantine.GroupDataResponse
import com.nunchuk.android.core.data.model.byzantine.HealthCheckRequest
import com.nunchuk.android.core.data.model.byzantine.HistoryPeriodResponse
import com.nunchuk.android.core.data.model.byzantine.RecurringPaymentListResponse
import com.nunchuk.android.core.data.model.byzantine.RecurringPaymentResponse
import com.nunchuk.android.core.data.model.byzantine.TotalAlertResponse
import com.nunchuk.android.core.data.model.byzantine.WalletConstraintsDataResponse
import com.nunchuk.android.core.data.model.byzantine.WalletHealthStatusResponse
import com.nunchuk.android.core.data.model.coin.CoinDataContent
import com.nunchuk.android.core.data.model.membership.BatchTransactionPayload
import com.nunchuk.android.core.data.model.membership.CalculateRequiredSignaturesResponse
import com.nunchuk.android.core.data.model.membership.CreateOrUpdateServerTransactionRequest
import com.nunchuk.android.core.data.model.membership.CreateOrUpdateWalletResponse
import com.nunchuk.android.core.data.model.membership.DesktopKeyRequest
import com.nunchuk.android.core.data.model.membership.GetWalletsResponse
import com.nunchuk.android.core.data.model.membership.HealthReminderRequest
import com.nunchuk.android.core.data.model.membership.HealthReminderResponse
import com.nunchuk.android.core.data.model.membership.KeyPolicyUpdateRequest
import com.nunchuk.android.core.data.model.membership.PeriodResponse
import com.nunchuk.android.core.data.model.membership.PermissionResponse
import com.nunchuk.android.core.data.model.membership.RandomizeBroadcastBatchTransactionsPayload
import com.nunchuk.android.core.data.model.membership.RequestDesktopKeyResponse
import com.nunchuk.android.core.data.model.membership.RequestSignatureTransactionRequest
import com.nunchuk.android.core.data.model.membership.ScheduleTransactionRequest
import com.nunchuk.android.core.data.model.membership.SignServerTransactionRequest
import com.nunchuk.android.core.data.model.membership.SignerServerDto
import com.nunchuk.android.core.data.model.membership.TransactionNoteResponse
import com.nunchuk.android.core.data.model.membership.TransactionResponse
import com.nunchuk.android.core.data.model.membership.TransactionsResponse
import com.nunchuk.android.core.data.model.membership.UpdatePrimaryOwnerRequest
import com.nunchuk.android.core.data.model.membership.WalletAliasRequest
import com.nunchuk.android.core.data.model.membership.WalletAliasResponse
import com.nunchuk.android.core.data.model.payment.CreateRecurringPaymentRequest
import com.nunchuk.android.core.data.model.replacement.WalletReplacementStatusResponse
import com.nunchuk.android.core.network.Data
import com.nunchuk.android.model.KeyResponse
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

internal interface GroupWalletApi {
    @PUT("/v1.1/group-wallets/groups/{group_id}/server-keys/{key_id_or_xfp}")
    suspend fun updateGroupServerKeys(
        @HeaderMap headers: Map<String, String>,
        @Path("key_id_or_xfp") keyId: String,
        @Path("group_id") groupId: String,
        @Query("derivation_path") derivationPath: String,
        @Query("draft") draft: Boolean = false,
        @Body body: KeyPolicyUpdateRequest,
    ): Data<CreateServerKeyResponse>

    @POST("/v1.1/group-wallets/groups/{group_id}/server-keys/{key_id_or_xfp}/calculate-required-signatures")
    suspend fun calculateRequiredSignaturesUpdateGroupServerKey(
        @Path("group_id") groupId: String,
        @Path("key_id_or_xfp") id: String,
        @Query("derivation_path") derivationPath: String,
        @Body payload: CreateServerKeysPayload,
    ): Data<CalculateRequiredSignaturesResponse>

    @POST("/v1.1/group-wallets/groups/{group_id}/draft-wallets/request-add-key")
    suspend fun requestAddKey(
        @Path("group_id") groupId: String,
        @Body payload: DesktopKeyRequest,
    ): Data<RequestDesktopKeyResponse>

    @GET("/v1.1/group-wallets/groups/{group_id}/draft-wallets/request-add-key/{request_id}")
    suspend fun getRequestAddKeyStatus(
        @Path("group_id") groupId: String,
        @Path("request_id") requestId: String,
    ): Data<RequestDesktopKeyResponse>

    @DELETE("/v1.1/group-wallets/groups/{group_id}/draft-wallets/request-add-key/{request_id}")
    suspend fun cancelRequestAddKey(
        @Path("group_id") groupId: String,
        @Path("request_id") requestId: String,
    ): Data<RequestDesktopKeyResponse>

    @POST("/v1.1/group-wallets/groups/{group_id}/draft-wallets/add-key")
    suspend fun addKeyToServer(
        @Path("group_id") groupId: String,
        @Body payload: SignerServerDto,
    ): Data<SignerServerDto>


    @POST("/v1.1/group-wallets/groups/{group_id}/server-keys")
    suspend fun createGroupServerKey(
        @Path("group_id") groupId: String, @Body payload: CreateServerKeysPayload,
    ): Data<CreateServerKeyResponse>

    @POST("/v1.1/group-wallets/groups/{group_id}/draft-wallets/set-server-key")
    suspend fun setGroupServerKey(
        @Path("group_id") groupId: String, @Body payload: Map<String, String>,
    ): Data<CreateServerKeyResponse>

    @GET("/v1.1/group-wallets/groups/{group_id}/server-keys/{key_id_or_xfp}")
    suspend fun getGroupServerKey(
        @Path("group_id") groupId: String,
        @Path("key_id_or_xfp") id: String,
        @Query("derivation_path") derivationPath: String,
    ): Data<CreateServerKeyResponse>

    @GET("/v1.1/group-wallets/groups/{group_id}/draft-wallets/current")
    suspend fun getDraftWallet(@Path("group_id") groupId: String): Data<DraftWalletResponse>

    @DELETE("/v1.1/group-wallets/groups/{group_id}/draft-wallets/current")
    suspend fun deleteDraftWallet(@Path("group_id") groupId: String): Data<Unit>

    @PUT("/v1.1/group-wallets/groups/{group_id}/draft-wallets/timelock")
    suspend fun createGroupDraftWalletTimelock(
        @Path("group_id") groupId: String,
        @Body payload: CreateTimelockPayload
    ): Data<DraftWalletResponse>

    @GET("/v1.1/group-wallets/permissions/default")
    suspend fun getPermissionGroupWallet(
        @Query("n") n: Int,
        @Query("m") m: Int,
        @Query("required_server_key") requiredServerKey: Boolean,
        @Query("allow_inheritance") allowInheritance: Boolean,
    ): Data<PermissionResponse>

    @POST("/v1.1/group-wallets/groups")
    suspend fun createGroup(@Body payload: CreateGroupRequest): Data<GroupDataResponse>

    @GET("/v1.1/group-wallets/configs/wallet-constraints")
    suspend fun getGroupWalletsConstraints(): Data<WalletConstraintsDataResponse>

    @GET("/v1.1/group-wallets/groups")
    suspend fun getGroups(): Data<GetGroupsResponse>

    @GET("/v1.1/group-wallets/groups/{group_id}")
    suspend fun getGroup(@Path("group_id") groupId: String): Data<GroupDataResponse>

    @POST("/v1.1/group-wallets/groups/{group_id}/members/calculate-requires-signatures")
    suspend fun calculateRequiredSignaturesEditMember(
        @Path("group_id") groupId: String,
        @Body payload: EditGroupMemberRequest.Body,
    ): Data<CalculateRequiredSignaturesResponse>

    @PUT("/v1.1/group-wallets/groups/{group_id}/members")
    suspend fun editGroupMember(
        @Path("group_id") groupId: String,
        @HeaderMap headers: Map<String, String>, @Body payload: EditGroupMemberRequest,
    ): Data<GroupDataResponse>

    @POST("/v1.1/group-wallets/groups/{group_id}/wallets/create-from-draft")
    suspend fun createGroupWallet(
        @Path("group_id") groupId: String,
        @Body payload: CreateDraftWalletRequest,
    ): Data<CreateOrUpdateWalletResponse>

    @GET("/v1.1/group-wallets/groups/{group_id}/wallets/current")
    suspend fun getGroupWallet(@Path("group_id") groupId: String): Data<CreateOrUpdateWalletResponse>

    @PUT("/v1.1/group-wallets/groups/{group_id}/members/accept")
    suspend fun groupMemberAcceptRequest(
        @Path("group_id") groupId: String,
    ): Data<Unit>

    @PUT("/v1.1/group-wallets/groups/{group_id}/members/deny")
    suspend fun groupMemberDenyRequest(
        @Path("group_id") groupId: String,
    ): Data<Unit>

    @GET("/v1.1/group-wallets/groups/{group_id}/alerts")
    suspend fun getAlerts(
        @Path("group_id") groupId: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int = TRANSACTION_PAGE_COUNT,
    ): Data<GroupAlertResponse>

    @PUT("/v1.1/group-wallets/groups/{group_id}/alerts/{alert_id}/mark-as-read")
    suspend fun markAlertAsRead(
        @Path("group_id") groupId: String,
        @Path("alert_id") alertId: String,
    ): Data<Unit>

    @PUT("/v1.1/group-wallets/groups/{group_id}/alerts/{alert_id}/dismiss")
    suspend fun dismissAlert(
        @Path("group_id") groupId: String,
        @Path("alert_id") alertId: String,
    ): Data<Unit>

    @GET("/v1.1/group-wallets/groups/{group_id}/alerts/total")
    suspend fun getAlertTotal(
        @Path("group_id") groupId: String,
    ): Data<TotalAlertResponse>

    @GET("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/dummy-transactions/{dummy_transaction_id}")
    suspend fun getDummyTransaction(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("dummy_transaction_id") transactionId: String,
    ): Data<DummyTransactionResponse>

    @PUT("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/dummy-transactions/{dummy_transaction_id}")
    suspend fun updateDummyTransaction(
        @HeaderMap headers: Map<String, String>,
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("dummy_transaction_id") transactionId: String,
    ): Data<DummyTransactionResponse>

    @DELETE("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/dummy-transactions/{dummy_transaction_id}")
    suspend fun deleteDummyTransaction(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("dummy_transaction_id") transactionId: String,
    ): Data<Unit>

    @POST("/v1.1/group-wallets/chat/{group_id}")
    suspend fun createGroupChat(
        @Path("group_id") groupId: String,
        @Body payload: CreateOrUpdateGroupChatRequest,
    ): Data<GroupChatDataResponse>

    @DELETE("/v1.1/group-wallets/chat/{group_id}/current")
    suspend fun deleteGroupChat(
        @Path("group_id") groupId: String,
    ): Data<Unit>

    @GET("/v1.1/group-wallets/chat")
    suspend fun getGroupChats(): Data<GroupChatListDataResponse>

    @GET("/v1.1/group-wallets/chat/{group_id}/current")
    suspend fun getGroupChat(
        @Path("group_id") groupId: String,
    ): Data<GroupChatDataResponse>

    @PUT("/v1.1/group-wallets/chat/{group_id}/current")
    suspend fun updateGroupChat(
        @Path("group_id") groupId: String,
        @Body body: CreateOrUpdateGroupChatRequest,
    ): Data<GroupChatDataResponse>

    @GET("/v1.1/group-wallets/chat/settings/history-periods")
    suspend fun getHistoryPeriods(): Data<HistoryPeriodResponse>

    @PUT("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}")
    suspend fun updateWallet(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletLocalId: String,
        @Body payload: UpdateWalletPayload,
    ): Data<CreateOrUpdateWalletResponse>

    @POST("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/calculate-required-signatures")
    suspend fun calculateRequiredSignaturesDeleteAssistedWallet(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
    ): Data<CalculateRequiredSignaturesResponse>

    @HTTP(
        method = "DELETE",
        path = "/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}",
        hasBody = true
    )
    suspend fun deleteAssistedWallet(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @HeaderMap headers: Map<String, String>,
        @Body payload: DeleteAssistedWalletRequest,
    ): Data<TransactionResponse>

    @POST("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/coin-control")
    suspend fun uploadCoinControlData(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Body payload: CoinDataContent,
    ): Data<Unit>

    @GET("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/coin-control")
    suspend fun getCoinControlData(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
    ): Data<CoinDataContent>

    @GET("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/transactions/{transaction_id}")
    suspend fun getTransaction(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("transaction_id") transactionId: String,
    ): Data<TransactionResponse>

    @DELETE("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/transactions/{transaction_id}")
    suspend fun deleteTransaction(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("transaction_id") transactionId: String,
    ): Data<TransactionResponse>

    @PUT("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/transactions/{transaction_id}")
    suspend fun updateTransaction(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("transaction_id") transactionId: String,
        @Body payload: CreateOrUpdateServerTransactionRequest,
    ): Data<TransactionResponse>

    @POST("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/transactions")
    suspend fun createTransaction(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Body payload: CreateOrUpdateServerTransactionRequest,
    ): Data<TransactionResponse>

    @POST("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/transactions/{transaction_id}/schedule")
    suspend fun scheduleTransaction(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("transaction_id") transactionId: String,
        @Body payload: ScheduleTransactionRequest,
    ): Data<TransactionResponse>

    @POST("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/transactions/{transaction_id}/request-signature")
    suspend fun requestSignatureTransaction(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("transaction_id") transactionId: String,
        @Body payload: RequestSignatureTransactionRequest,
    ): Data<*>

    @POST("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/transactions/{transaction_id}/sync")
    suspend fun syncTransaction(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("transaction_id") transactionId: String,
        @Body payload: SyncTransactionRequest,
    ): Data<TransactionResponse>

    @DELETE("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/transactions/{transaction_id}/schedule")
    suspend fun deleteScheduleTransaction(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("transaction_id") transactionId: String,
    ): Data<TransactionResponse>

    @POST("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/transactions/{transaction_id}/sign")
    suspend fun signServerTransaction(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("transaction_id") transactionId: String,
        @Body payload: SignServerTransactionRequest,
    ): Data<TransactionResponse>

    @GET("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/health")
    suspend fun getWalletHealthStatus(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
    ): Data<WalletHealthStatusResponse>

    @POST("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/health/{xfp}/request-health-check")
    suspend fun requestHealthCheck(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("xfp") xfp: String,
    ): Data<Unit>

    @POST("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/health/{xfp}/health-check")
    suspend fun healthCheck(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("xfp") xfp: String,
        @Query("draft") draft: Boolean,
        @Body request: HealthCheckRequest,
    ): Data<DummyTransactionResponse>

    @GET("/v1.1/group-wallets/wallets")
    suspend fun getWallets(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("statuses") statuses: List<String>,
    ): Data<GetWalletsResponse>

    @GET("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/transactions?limit=${TRANSACTION_PAGE_COUNT}&statuses=PENDING_SIGNATURES,READY_TO_BROADCAST&types=STANDARD,SCHEDULED,CLAIMING,ROLLOVER,RECURRING")
    suspend fun getTransactionsToSync(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Query("offset") offset: Int,
    ): Data<TransactionsResponse>

    @GET("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/transactions?limit=${TRANSACTION_PAGE_COUNT}&statuses=CANCELED&types=STANDARD,SCHEDULED,CLAIMING,ROLLOVER,RECURRING")
    suspend fun getTransactionsToDelete(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Query("offset") offset: Int,
    ): Data<TransactionsResponse>

    @PUT("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/dummy-transactions/{dummy_transaction_id}/finalize")
    suspend fun finalizeDummyTransaction(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("dummy_transaction_id") transactionId: String,
    ): Data<DummyTransactionResponse>

    @GET("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/transactions/notes?limit=${TRANSACTION_PAGE_COUNT}&statuses=CONFIRMED,NETWORK_REJECTED")
    suspend fun getConfirmedAndRejectedTransactions(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Query("offset") offset: Int,
    ): Data<TransactionNoteResponse>

    @GET("/v1.1/group-wallets/lockdown/period")
    suspend fun getLockdownPeriod(): Data<PeriodResponse>

    @POST("/v1.1/group-wallets/lockdown/lock")
    suspend fun lockdownUpdate(
        @HeaderMap headers: Map<String, String>, @Body payload: LockdownUpdateRequest,
    ): Data<Unit>

    @POST("/v1.1/group-wallets/lockdown/calculate-required-signatures")
    suspend fun calculateRequiredSignaturesLockdown(
        @Body payload: LockdownUpdateRequest.Body,
    ): Data<CalculateRequiredSignaturesResponse>

    @POST("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/recurring-payment")
    suspend fun createRecurringPayment(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Query("draft") draft: Boolean = true,
        @Body request: CreateRecurringPaymentRequest,
    ): Data<DummyTransactionResponse>

    @GET("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/recurring-payment")
    suspend fun getRecurringPayments(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
    ): Data<RecurringPaymentListResponse>

    @GET("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/recurring-payment/{recurring_payment_id}")
    suspend fun getRecurringPayment(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("recurring_payment_id") recurringPaymentId: String,
    ): Data<RecurringPaymentResponse>

    @HTTP(
        method = "DELETE",
        path = "/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/recurring-payment/{recurring_payment_id}",
        hasBody = true
    )
    suspend fun deleteRecurringPayment(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("recurring_payment_id") recurringPaymentId: String,
        @Body request: CreateRecurringPaymentRequest,
    ): Data<DummyTransactionResponse>

    @PUT("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/primary-owner")
    suspend fun updatePrimaryOwner(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Body payload: UpdatePrimaryOwnerRequest,
    ): Data<Unit>

    @PUT("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/transactions/{transaction_id}/rbf")
    suspend fun replaceTransaction(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("transaction_id") transactionId: String,
        @Body payload: CreateOrUpdateServerTransactionRequest,
    ): Data<TransactionResponse>


    @GET("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/alias")
    suspend fun getWalletAlias(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
    ): Data<WalletAliasResponse>

    @PUT("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/alias")
    suspend fun setWalletAlias(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Body request: WalletAliasRequest,
    ): Data<Unit>

    @DELETE("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/alias")
    suspend fun deleteWalletAlias(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
    ): Data<Unit>

    @GET("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/health/reminder")
    suspend fun getHealthReminders(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
    ): Data<HealthReminderResponse>

    @PUT("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/health/reminder")
    suspend fun updateHealthReminder(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Body payload: HealthReminderRequest,
    ): Data<Unit>

    @DELETE("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/health/reminder")
    suspend fun deleteHealthReminder(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Query("xfps") xfps: List<String>,
    ): Data<Unit>

    @DELETE("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/health/reminder/{xfp}")
    suspend fun skipHealthReminder(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("xfp") xfp: String,
    ): Data<Unit>

    @POST("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/replacement/{xfp}")
    suspend fun initReplaceKey(
        @Header("Verify-token") verifyToken: String,
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("xfp") xfp: String,
    ): Data<Unit>

    @DELETE("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/replacement/{xfp}")
    suspend fun cancelReplaceKey(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("xfp") xfp: String,
    ): Data<Unit>

    @POST("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/replacement/{xfp}/replace")
    suspend fun replaceKey(
        @Header("Verify-token") verifyToken: String,
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("xfp") xfp: String,
        @Body payload: SignerServerDto
    ): Data<Unit>

    @POST("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/replacement/finalize")
    suspend fun finalizeReplaceWallet(
        @Header("Verify-token") verifyToken: String,
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
    ): Data<CreateOrUpdateWalletResponse>

    @GET("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/replacement/status")
    suspend fun getReplaceWalletStatus(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
    ): Data<WalletReplacementStatusResponse>

    @DELETE("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/replacement/reset")
    suspend fun resetReplaceWallet(
        @Header("Verify-token") verifyToken: String,
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
    ): Data<Unit>

    @PUT("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/replacement/configs")
    suspend fun updateReplaceWalletConfigs(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Body payload: Map<String, Boolean>,
    ): Data<Unit>

    @POST("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/replacement/timelock")
    suspend fun replaceTimelock(
        @Header("Verify-token") verifyToken: String,
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Body payload: CreateTimelockPayload,
    ): Data<DraftWalletResponse>

    @POST("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/transactions/batch")
    suspend fun batchTransactions(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Body payload: BatchTransactionPayload
    ): Data<TransactionsResponse>

    @POST("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/transactions/batch/randomize-broadcast")
    suspend fun randomizeBroadcastBatchTransactions(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Body payload: RandomizeBroadcastBatchTransactionsPayload
    ): Data<Unit>

    @Multipart
    @POST("/v1.1/group-wallets/groups/{group_id}/draft-wallets/upload-backup")
    suspend fun uploadBackupKey(
        @Path("group_id") groupId: String,
        @Part("key_name") keyName: RequestBody,
        @Part("key_type") keyType: RequestBody,
        @Part("key_xfp") keyXfp: RequestBody,
        @Part("card_id") cardId: RequestBody,
        @Part image: MultipartBody.Part,
    ): Data<KeyResponse>

    @POST("/v1.1/group-wallets/groups/{group_id}/draft-wallets/{xfp}/download-backup")
    suspend fun downloadBackup(
        @Path("xfp") xfp: String,
        @Path("group_id") groupId: String,
    ): Data<KeyResponse>

    @POST("/v1.1/group-wallets/groups/{group_id}/draft-wallets/{xfp}/verify")
    suspend fun setKeyVerified(
        @Path("group_id") groupId: String,
        @Path("xfp") keyId: String, @Body payload: KeyVerifiedRequest
    ): Data<Unit>

    @Multipart
    @POST("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/replacement/upload-backup")
    suspend fun uploadBackupKeyReplacement(
        @Header("Verify-token") verifyToken: String,
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Part("key_name") keyName: RequestBody,
        @Part("key_type") keyType: RequestBody,
        @Part("key_xfp") keyXfp: RequestBody,
        @Part("card_id") cardId: RequestBody,
        @Part image: MultipartBody.Part,
    ): Data<KeyResponse>

    @POST("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/replacement/{xfp}/download-backup")
    suspend fun downloadBackupReplacement(
        @Header("Verify-token") verifyToken: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("xfp") xfp: String,
        @Path("group_id") groupId: String,
    ): Data<KeyResponse>

    @POST("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/replacement/{xfp}/verify")
    suspend fun setKeyVerifiedReplacement(
        @Header("Verify-token") verifyToken: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("group_id") groupId: String,
        @Path("xfp") keyId: String, @Body payload: KeyVerifiedRequest
    ): Data<Unit>

    @DELETE("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/replacement/{xfp}/remove")
    suspend fun removeKeyReplacement(
        @Header("Verify-token") verifyToken: String,
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String,
        @Path("xfp") xfp: String,
    ): Data<Unit>

    @GET("/v1.1/group-wallets/wallets/deleted")
    suspend fun getDeletedGroupWallets(): Data<DeletedGroupWalletsResponse>

    @POST("/v1.1/group-wallets/groups/{group_id}/wallets/{wallet_id_or_local_id}/change-timelock-type")
    suspend fun changeTimelockType(
        @Path("group_id") groupId: String,
        @Path("wallet_id_or_local_id") walletId: String
    ): Data<DraftWalletResponse>
}