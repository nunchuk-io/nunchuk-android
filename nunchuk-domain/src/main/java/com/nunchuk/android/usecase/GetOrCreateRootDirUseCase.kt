package com.nunchuk.android.usecase

import com.nunchuk.android.util.FileHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface GetOrCreateRootDirUseCase {
    fun execute(): Flow<String>
}

internal class GetOrCreateRootDirUseCaseImpl @Inject constructor(
    private val filerHelper: FileHelper
) : GetOrCreateRootDirUseCase {

    override fun execute() = flow {
        emit(
            filerHelper.getOrCreateNunchukRootDir()
        )
    }
}
