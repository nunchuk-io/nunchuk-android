package com.nunchuk.android.core.data.api

import com.nunchuk.android.core.data.model.sharedwallet.GroupWalletResponse
import com.nunchuk.android.core.data.model.sharedwallet.JoinSharedWalletRequest
import com.nunchuk.android.core.network.Data
import retrofit2.http.Body
import retrofit2.http.POST

internal interface SharedWalletApi {
    @POST("/v1.1/shared-wallets")
    suspend fun joinGroupSharedWallet(@Body request: JoinSharedWalletRequest): Data<GroupWalletResponse>

}