package com.nunchuk.android.usecase

import com.nunchuk.android.util.FileHelper
import javax.inject.Inject

interface GetOrCreateRootDirUseCase {
    suspend fun execute(): String
}

internal class GetOrCreateRootDirUseCaseImpl @Inject constructor(
    private val filerHelper: FileHelper
) : GetOrCreateRootDirUseCase {

    override suspend fun execute() = filerHelper.getOrCreateNunchukRootDir()

}
