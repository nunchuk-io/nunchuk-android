package com.nunchuk.android.api.key

import com.nunchuk.android.core.network.Data
import com.nunchuk.android.model.KeyResponse
import com.nunchuk.android.model.KeyVerifiedRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface KeyApi {
    @Multipart
    @POST("/v1.1/user-wallets/user-keys/upload-backup")
    suspend fun uploadBackupKey(
        @Part("key_name") keyName: RequestBody,
        @Part("key_type") keyType: RequestBody,
        @Part("key_xfp") keyXfp: RequestBody,
        @Part image: MultipartBody.Part,
    ): Data<KeyResponse>

    @POST("/v1.1/user-wallets/user-keys/{key_id}/verify")
    suspend fun setKeyVerified(@Path("key_id") keyId: String, @Body payload: KeyVerifiedRequest) : Data<Unit>
}