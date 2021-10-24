package com.nunchuk.android.core.matrix

import com.nunchuk.android.core.data.MatrixAPIRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.ResponseBody
import javax.inject.Inject

/*
* Android Matrix SDK does not have a native upload func, so we have to call rest api
* */
interface DownloadFileUseCase {
    fun execute(
        serverName: String,
        mediaId: String
    ): Flow<ResponseBody>
}

internal class DownloadFileUseCaseImpl @Inject constructor(
    private val matrixAPIRepository: MatrixAPIRepository
) : DownloadFileUseCase {

    override fun execute(
        serverName: String,
        mediaId: String
    ) = matrixAPIRepository.download(serverName, mediaId)
}
