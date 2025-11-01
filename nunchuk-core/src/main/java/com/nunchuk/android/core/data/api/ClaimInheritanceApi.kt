package com.nunchuk.android.core.data.api

import com.nunchuk.android.core.data.model.InheritanceClaimingInitRequest
import com.nunchuk.android.core.data.model.membership.InheritanceClaimingInitResponse
import com.nunchuk.android.core.network.Data
import retrofit2.http.Body
import retrofit2.http.POST

internal interface ClaimInheritanceApi {
    @POST("/v1.1/user-wallets/inheritance/claiming/init")
    suspend fun inheritanceClaimingInit(
        @Body payload: InheritanceClaimingInitRequest
    ): Data<InheritanceClaimingInitResponse>
}