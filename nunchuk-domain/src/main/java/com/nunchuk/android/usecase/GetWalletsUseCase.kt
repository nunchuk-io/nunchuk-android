package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface GetWalletsUseCase {
    suspend fun execute(): Result<List<Wallet>>
}

internal class GetWalletsUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), GetWalletsUseCase {

    override suspend fun execute() = exe { nativeSdk.getWallets() }

}

