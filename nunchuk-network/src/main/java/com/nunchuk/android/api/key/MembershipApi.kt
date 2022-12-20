package com.nunchuk.android.api.key

import com.nunchuk.android.core.network.Data
import com.nunchuk.android.model.MembershipSubscription
import com.nunchuk.android.model.VerifiedPasswordTokenRequest
import com.nunchuk.android.model.VerifiedPasswordTokenResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MembershipApi {
    @GET("/v1.1/subscriptions/current")
    suspend fun getCurrentSubscription(): Data<MembershipSubscription>

    @GET("/v1.1/subscriptions/testnet")
    suspend fun getTestnetCurrentSubscription(): Data<MembershipSubscription>

    @POST("/v1.1/passport/verified-password-token/{target_action}")
    suspend fun verifiedPasswordToken(
        @Path("target_action") targetAction: String,
        @Body payload: VerifiedPasswordTokenRequest
    ): Data<VerifiedPasswordTokenResponse>
}