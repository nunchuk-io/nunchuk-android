package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface GetRemoteSignersUseCase {
    suspend fun execute(): Result<List<SingleSigner>>
}

internal class GetRemoteSignersUseCaseImpl @Inject constructor(
    val libNunchukFacade: LibNunchukFacade
) : BaseUseCase(), GetRemoteSignersUseCase {

    override suspend fun execute() = exe { libNunchukFacade.getRemoteSigners() }

}