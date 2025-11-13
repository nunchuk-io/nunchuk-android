package com.nunchuk.android.core.data.api

import com.nunchuk.android.core.data.model.InheritanceClaimingInitRequest
import com.nunchuk.android.core.data.model.InheritanceClaimingDownloadWalletRequest
import com.nunchuk.android.core.data.model.membership.InheritanceClaimingInitResponse
import com.nunchuk.android.core.data.model.membership.InheritanceClaimingDownloadWalletResponse
import com.nunchuk.android.core.network.Data
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

internal interface ClaimInheritanceApi {
    @POST("/v1.1/user-wallets/inheritance/claiming/init")
    suspend fun inheritanceClaimingInit(
        @Body payload: InheritanceClaimingInitRequest
    ): Data<InheritanceClaimingInitResponse>

    @POST("/v1.1/user-wallets/inheritance/claiming/download-wallet")
    suspend fun inheritanceClaimingDownloadWallet(
        @Body payload: InheritanceClaimingDownloadWalletRequest
    ): Data<InheritanceClaimingDownloadWalletResponse>

    @GET("/v1.1/user-wallets/claiming-wallets/{local_id}")
    suspend fun getClaimingWallet(
        @Path("local_id") localId: String
    ): Data<InheritanceClaimingDownloadWalletResponse>
}