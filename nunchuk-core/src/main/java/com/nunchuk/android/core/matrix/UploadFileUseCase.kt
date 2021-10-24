package com.nunchuk.android.core.matrix

import com.nunchuk.android.core.api.MatrixUploadFileResponse
import com.nunchuk.android.core.data.MatrixAPIRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/*
* Android Matrix SDK does not have a native upload func, so we have to call rest api
* */
interface UploadFileUseCase {
    fun execute(
        fileName: String,
        fileType: String,
        fileData: ByteArray
    ): Flow<MatrixUploadFileResponse>
}

internal class UploadFileUseCaseImpl @Inject constructor(
    private val matrixAPIRepository: MatrixAPIRepository
) : UploadFileUseCase {

    override fun execute(fileName: String, fileType: String, fileData: ByteArray) =
        matrixAPIRepository.upload(fileName, fileType, fileData)
}
