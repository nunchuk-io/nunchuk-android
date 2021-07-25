package com.nunchuk.android.usecase

import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface GetWalletsUseCase {
    suspend fun execute(): Flow<List<Wallet>>
}

internal class GetWalletsUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : GetWalletsUseCase {

    override suspend fun execute() = flow { emit(nativeSdk.getWallets()) }

}

