package com.nunchuk.android.core.data.api

import com.nunchuk.android.core.data.model.banner.AssistedContentResponse
import com.nunchuk.android.core.data.model.banner.BannerListResponse
import com.nunchuk.android.core.data.model.banner.SubmitEmailViewAssistedWalletRequest
import com.nunchuk.android.core.network.Data
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

internal interface BannerApi {
    @POST("/v1.1/banners/pages/submit-email")
    suspend fun submitEmail(@Body payload: SubmitEmailViewAssistedWalletRequest): Data<Unit>

    @GET("/v1.1/banners/pages/assisted-wallet")
    suspend fun getAssistedWalletBannerContent(@Query("reminder_id") requestId: String): Data<AssistedContentResponse>

    @GET("/v1.1/banners/reminders/home")
    suspend fun getBanners(): Data<BannerListResponse>
}