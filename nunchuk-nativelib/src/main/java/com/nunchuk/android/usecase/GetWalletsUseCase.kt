package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface GetWalletsUseCase {
    suspend fun execute(): Result<List<Wallet>>
}

internal class GetWalletsUseCaseImpl @Inject constructor(
    private val facade: LibNunchukFacade
) : BaseUseCase(), GetWalletsUseCase {

    override suspend fun execute() = exe { facade.getWallets() }

}

