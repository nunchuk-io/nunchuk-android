package com.nunchuk.android.api.key

import com.nunchuk.android.core.network.Data
import com.nunchuk.android.model.MembershipSubscription
import retrofit2.http.GET

interface MembershipApi {
    @GET("/v1.1/subscriptions/current")
    suspend fun getCurrentSubscription(): Data<MembershipSubscription>

    @GET("/v1.1/subscriptions/testnet")
    suspend fun getTestnetCurrentSubscription(): Data<MembershipSubscription>
}