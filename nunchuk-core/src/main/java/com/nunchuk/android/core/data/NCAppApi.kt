package com.nunchuk.android.core.data

import com.nunchuk.android.core.data.model.AppUpdateResponse
import com.nunchuk.android.core.network.Data
import retrofit2.http.*

interface NCAppApi {

    @GET("app/check-for-update")
    suspend fun checkAppUpdate(): Data<AppUpdateResponse>

}