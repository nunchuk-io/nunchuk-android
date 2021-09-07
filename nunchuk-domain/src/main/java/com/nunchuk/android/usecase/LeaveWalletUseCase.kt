package com.nunchuk.android.usecase

import com.nunchuk.android.model.NunchukMatrixEvent
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface LeaveWalletUseCase {
    fun execute(roomId: String, joinEventId: String, reason: String = ""): Flow<NunchukMatrixEvent>
}

internal class LeaveWalletUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : LeaveWalletUseCase {

    override fun execute(roomId: String, joinEventId: String, reason: String) = flow {
        emit(
            nativeSdk.leaveSharedWallet(roomId = roomId, joinEventId = joinEventId, reason = reason)
        )
    }

}