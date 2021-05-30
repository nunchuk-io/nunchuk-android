package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface GetChainTipUseCase {
    suspend fun execute(): Result<Int>
}

internal class GetChainTipUseCaseImpl @Inject constructor(
    private val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), GetChainTipUseCase {

    override suspend fun execute() = exe {
        nunchukFacade.getChainTip()
    }

}