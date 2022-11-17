/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.core.data.api

import com.nunchuk.android.core.data.model.MatrixUploadFileResponse
import com.nunchuk.android.core.data.model.SyncStateMatrixResponse
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