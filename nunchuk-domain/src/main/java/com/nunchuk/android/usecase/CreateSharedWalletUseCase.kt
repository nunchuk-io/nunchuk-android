package com.nunchuk.android.usecase

import com.nunchuk.android.model.NunchukMatrixEvent
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface CreateSharedWalletUseCase {
    fun execute(roomId: String): Flow<NunchukMatrixEvent>
}

internal class CreateSharedWalletUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : CreateSharedWalletUseCase {

    override fun execute(roomId: String) = flow {
        emit(
            nativeSdk.createSharedWallet(roomId)
        )
    }

}