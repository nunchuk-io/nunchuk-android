package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface GetRemoteSignerUseCase {
    suspend fun execute(): Result<SingleSigner>
}

internal class GetRemoteSignerUseCaseImpl @Inject constructor(
    private val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), GetRemoteSignerUseCase {

    override suspend fun execute() = exe { nunchukFacade.getRemoteSigner() }

}