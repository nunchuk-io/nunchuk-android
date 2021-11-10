package com.nunchuk.android.usecase

import android.content.Context
import com.nunchuk.android.model.Result
import javax.inject.Inject

interface CreateShareFileUseCase {
    suspend fun execute(fileId: String): Result<String>
}

internal class CreateShareFileUseCaseImpl @Inject constructor(
    private val contextProvider: ContextProvider
) : BaseUseCase(), CreateShareFileUseCase {

    override suspend fun execute(fileId: String) = exe {
        "${contextProvider.context.getExternalFilesDir(null)}/$fileId"
    }

}

internal class ContextProvider @Inject constructor(val context: Context)