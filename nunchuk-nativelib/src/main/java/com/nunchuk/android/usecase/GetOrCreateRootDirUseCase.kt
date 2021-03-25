package com.nunchuk.android.usecase

import com.nunchuk.android.core.util.FileHelper
import io.reactivex.Single
import javax.inject.Inject

interface GetOrCreateRootDirUseCase {
    fun execute(): Single<String>
}

internal class GetOrCreateRootDirUseCaseImpl @Inject constructor(
    private val filerHelper: FileHelper
) : GetOrCreateRootDirUseCase {

    override fun execute() = Single.fromCallable(filerHelper::getOrCreateNunchukRootDir)

}
