package com.nunchuk.android.core.domain

import com.nunchuk.android.core.util.saveToFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ResponseBody
import java.io.File
import javax.inject.Inject

interface SaveCacheFileUseCase {
    fun execute(
        data: ResponseBody,
        path: String
    ): Flow<String>
}

internal class SaveCacheFileUseCaseImpl @Inject constructor(
) : SaveCacheFileUseCase {

    override fun execute(
        data: ResponseBody,
        path: String
    ) = flow {
        val saveFile = File(path)
        data.byteStream().saveToFile(saveFile.path)
        emit(
            saveFile.absolutePath
        )
    }.flowOn(Dispatchers.IO)

}