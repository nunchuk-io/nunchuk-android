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

package com.nunchuk.android.core.repository

import android.content.Context
import com.nunchuk.android.core.data.api.MatrixAPI
import com.nunchuk.android.core.data.model.MatrixUploadFileResponse
import com.nunchuk.android.core.data.model.SyncStateMatrixResponse
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.saveToFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

interface MatrixAPIRepository {
    fun upload(
        fileName: String,
        fileType: String,
        fileData: ByteArray
    ): Flow<MatrixUploadFileResponse>

    suspend fun download(
        serverName: String,
        mediaId: String
    ): String

    fun syncState(): Flow<SyncStateMatrixResponse>
}

internal class MatrixAPIRepositoryImpl @Inject constructor(
    private val matrixAPI: MatrixAPI,
    private val sessionHolder: SessionHolder,
    @ApplicationContext private val context: Context,
) : MatrixAPIRepository {

    override fun upload(
        fileName: String,
        fileType: String,
        fileData: ByteArray
    ) = flow {
        emit(
            matrixAPI.upload(
                contentType = fileType,
                token = "Bearer ${sessionHolder.getSafeActiveSession()?.sessionParams?.credentials?.accessToken.orEmpty()}",
                fileName = fileName,
                body = fileData.toRequestBody(contentType = fileType.toMediaType())
            )
        )
    }

    override suspend fun download(serverName: String, mediaId: String) : String {
        val body = matrixAPI.download(serverName, mediaId)
        val filePath =
            context.externalCacheDir.toString() + File.separator + "FileBackup" + System.currentTimeMillis()
        body.byteStream().saveToFile(filePath)
        return filePath
    }

    override fun syncState() = flow {
        emit(
            matrixAPI.syncState(
                token = "Bearer ${sessionHolder.getSafeActiveSession()?.sessionParams?.credentials?.accessToken.orEmpty()}"
            )
        )
    }
}