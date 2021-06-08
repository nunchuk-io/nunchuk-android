package com.nunchuk.android.usecase

import android.os.Environment
import com.nunchuk.android.model.Result
import javax.inject.Inject

interface CreateShareFileUseCase {
    suspend fun execute(fileId: String): Result<String>
}

internal class CreateShareFileUseCaseImpl @Inject constructor(
) : BaseUseCase(), CreateShareFileUseCase {

    override suspend fun execute(fileId: String) = exe {
        @Suppress("DEPRECATION")
        "${Environment.getExternalStorageDirectory()}/$fileId"
    }

}