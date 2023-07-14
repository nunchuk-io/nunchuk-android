package com.nunchuk.android.core.data.api

import com.nunchuk.android.core.data.model.CreateServerKeyResponse
import com.nunchuk.android.core.data.model.CreateServerKeysPayload
import com.nunchuk.android.core.data.model.byzantine.CreateGroupRequest
import com.nunchuk.android.core.data.model.byzantine.DraftWalletResponse
import com.nunchuk.android.core.data.model.byzantine.EditGroupMemberRequest
import com.nunchuk.android.core.data.model.byzantine.GetGroupsResponse
import com.nunchuk.android.core.data.model.byzantine.GroupAlertResponse
import com.nunchuk.android.core.data.model.byzantine.GroupWalletDataResponse
import com.nunchuk.android.core.data.model.byzantine.WalletConstraintsDataResponse
import com.nunchuk.android.core.data.model.membership.CalculateRequiredSignaturesResponse
import com.nunchuk.android.core.data.model.membership.CreateOrUpdateWalletResponse
import com.nunchuk.android.core.data.model.membership.DesktopKeyRequest
import com.nunchuk.android.core.data.model.membership.KeyPolicyUpdateRequest
import com.nunchuk.android.core.data.model.membership.PermissionResponse
import com.nunchuk.android.core.data.model.membership.RequestDesktopKeyResponse
import com.nunchuk.android.core.data.model.membership.SignerServerDto
import com.nunchuk.android.core.network.Data
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

internal interface GroupWalletApi {
    @PUT("/v1.1/group-wallets/groups/{group_id}/server-keys/{key_id_or_xfp}")
    suspend fun updateGroupServerKeys(
        @HeaderMap headers: Map<String, String>,
        @Path("key_id_or_xfp") keyId: String,
        @Path("group_id") groupId: String,
        @Body body: KeyPolicyUpdateRequest
    ): Data<CreateServerKeyResponse>

    @POST("/v1.1/group-wallets/groups/{group_id}/server-keys/{key_id_or_xfp}/calculate-required-signatures")
    suspend fun calculateRequiredSignaturesUpdateGroupServerKey(
        @Path("group_id") groupId: String,
        @Path("key_id_or_xfp") id: String,
        @Body payload: CreateServerKeysPayload
    ): Data<CalculateRequiredSignaturesResponse>

    @POST("/v1.1/group-wallets/groups/{group_id}/draft-wallets/request-add-key")
    suspend fun requestAddKey(
        @Path("group_id") groupId: String,
        @Body payload: DesktopKeyRequest
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
        @Body payload: SignerServerDto
    ): Data<SignerServerDto>


    @POST("/v1.1/group-wallets/groups/{group_id}/server-keys")
    suspend fun createGroupServerKey(
        @Path("group_id") groupId: String, @Body payload: CreateServerKeysPayload
    ): Data<CreateServerKeyResponse>

    @POST("/v1.1/group-wallets/groups/{group_id}/draft-wallets/set-server-key")
    suspend fun setGroupServerKey(
        @Path("group_id") groupId: String, @Body payload: Map<String, String>
    ): Data<CreateServerKeyResponse>

    @GET("/v1.1/group-wallets/groups/{group_id}/server-keys/{key_id_or_xfp}")
    suspend fun getGroupServerKey(
        @Path("group_id") groupId: String, @Path("key_id_or_xfp") id: String,
    ): Data<CreateServerKeyResponse>

    @GET("/v1.1/group-wallets/groups/{group_id}/draft-wallets/current")
    suspend fun getDraftWallet(@Path("group_id") groupId: String): Data<DraftWalletResponse>

    @DELETE("/v1.1/group-wallets/groups/{group_id}/draft-wallets/current")
    suspend fun deleteDraftWallet(@Path("group_id") groupId: String)


    @GET("/v1.1/group-wallets/permissions/default")
    suspend fun getPermissionGroupWallet(): Data<PermissionResponse>

    @POST("/v1.1/group-wallets/groups")
    suspend fun createGroup(@Body payload: CreateGroupRequest): Data<GroupWalletDataResponse>

    @GET("/v1.1/group-wallets/configs/wallet-constraints")
    suspend fun getGroupWalletsConstraints(): Data<WalletConstraintsDataResponse>

    @GET("/v1.1/group-wallets/groups")
    suspend fun getGroups(): Data<GetGroupsResponse>

    @GET("/v1.1/group-wallets/groups/{group_id}")
    suspend fun getGroupWalletById(@Path("group_id") groupId: String): Data<GroupWalletDataResponse>

    @POST("/v1.1/group-wallets/groups/{group_id}/members/calculate-requires-signatures")
    suspend fun calculateRequiredSignaturesEditMember(
        @Path("group_id") groupId: String,
        @Body payload: EditGroupMemberRequest.Body
    ): Data<CalculateRequiredSignaturesResponse>

    @PUT("/v1.1/group-wallets/groups/{group_id}/members")
    suspend fun editGroupMember(
        @Path("group_id") groupId: String,
        @HeaderMap headers: Map<String, String>, @Body payload: EditGroupMemberRequest
    ): Data<GroupWalletDataResponse>

    @POST("/v1.1/group-wallets/groups/{group_id}/wallets/create-from-draft")
    suspend fun createGroupWallet(
        @Path("group_id") groupId: String,
        @Body payload: Map<String, String>
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
        @Query("limit") limit: Int = TRANSACTION_PAGE_COUNT
    ): Data<GroupAlertResponse>
}