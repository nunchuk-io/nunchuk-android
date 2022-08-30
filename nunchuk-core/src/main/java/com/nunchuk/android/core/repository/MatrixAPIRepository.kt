package com.nunchuk.android.core.repository

import com.nunchuk.android.core.data.api.MatrixAPI
import com.nunchuk.android.core.data.model.MatrixUploadFileResponse
import com.nunchuk.android.core.data.model.SyncStateMatrixResponse
import com.nunchuk.android.core.matrix.SessionHolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import javax.inject.Inject

interface MatrixAPIRepository {
    fun upload(
        fileName: String,
        fileType: String,
        fileData: ByteArray
    ): Flow<MatrixUploadFileResponse>

    fun download(
        serverName: String,
        mediaId: String
    ): Flow<ResponseBody>

    fun syncState(): Flow<SyncStateMatrixResponse>
}

internal class MatrixAPIRepositoryImpl @Inject constructor(
    private val matrixAPI: MatrixAPI,
    private val sessionHolder: SessionHolder
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

    override fun download(serverName: String, mediaId: String) = flow {
        emit(
            matrixAPI.download(serverName, mediaId)
        )
    }

    override fun syncState() = flow {
        emit(
            matrixAPI.syncState(
                token = "Bearer ${sessionHolder.getSafeActiveSession()?.sessionParams?.credentials?.accessToken.orEmpty()}"
            )
        )
    }
}