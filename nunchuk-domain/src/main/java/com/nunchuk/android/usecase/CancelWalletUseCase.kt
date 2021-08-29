package com.nunchuk.android.usecase

import com.nunchuk.android.model.NunchukMatrixEvent
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface CancelWalletUseCase {
    fun execute(roomId: String, reason: String): Flow<NunchukMatrixEvent>
}

internal class CancelWalletUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : CancelWalletUseCase {

    override fun execute(roomId: String, reason: String) = flow {
        emit(
            nativeSdk.cancelSharedWallet(roomId, reason)
        )
    }

}