package com.nunchuk.android.core.data.api

import com.nunchuk.android.core.data.model.sharedwallet.CreateInvitationRequest
import com.nunchuk.android.core.data.model.sharedwallet.GroupInvitationListResponse
import com.nunchuk.android.core.data.model.sharedwallet.GroupWalletResponse
import com.nunchuk.android.core.data.model.sharedwallet.InvitationListResponse
import com.nunchuk.android.core.data.model.sharedwallet.JoinSharedWalletRequest
import com.nunchuk.android.core.network.Data
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

internal interface SharedWalletApi {
    @POST("/v1.1/shared-wallets")
    suspend fun joinGroupSharedWallet(@Body request: JoinSharedWalletRequest): Data<GroupWalletResponse>

    @POST("/v1.1/shared-wallets/invitations")
    suspend fun createInvitation(@Body request: CreateInvitationRequest): Data<InvitationListResponse>

    @GET("/v1.1/shared-wallets/invitations/groups/{group_id}")
    suspend fun getGroupInvitations(@Path("group_id") groupId: String): Data<GroupInvitationListResponse>

    @DELETE("/v1.1/shared-wallets/invitations/{invitation_id}")
    suspend fun removeInvitation(@Path("invitation_id") invitationId: String): Data<Unit>

    @GET("/v1.1/shared-wallets/invitations")
    suspend fun getInvitations(): Data<InvitationListResponse>

    @POST("/v1.1/shared-wallets/invitations/{invitation_id}/deny")
    suspend fun denyInvitation(@Path("invitation_id") invitationId: String): Data<Unit>
}