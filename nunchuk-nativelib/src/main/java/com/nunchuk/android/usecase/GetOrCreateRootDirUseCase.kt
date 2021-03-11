package com.nunchuk.android.usecase

import com.nunchuk.android.core.util.FileUtil
import io.reactivex.Single
import javax.inject.Inject

interface GetOrCreateRootDirUseCase {
    fun execute(): Single<String>
}

internal class GetOrCreateRootDirUseCaseImpl @Inject constructor() : GetOrCreateRootDirUseCase {

    override fun execute() = Single.fromCallable(FileUtil::getOrCreateNunchukRootDir)

}
