package com.nunchuk.android.core.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Header
import retrofit2.http.Streaming

interface MatrixAPI {

    @POST("_matrix/media/r0/upload")
    suspend fun upload(
        @Header("Content-Type") contentType : String,
        @Header("Authorization") token : String,
        @Query("filename") fileName: String,
        @Body body: RequestBody
    ): MatrixUploadFileResponse

    @Streaming
    @GET("_matrix/media/r0/download/{serverName}/{mediaId}")
    suspend fun download(
        @Path("serverName") serverName: String,
        @Path("mediaId") mediaId: String,
    ): ResponseBody

    @GET("_matrix/client/r0/sync")
    suspend fun syncState(
        @Header("Authorization") token: String
    ): SyncStateMatrixResponse
}