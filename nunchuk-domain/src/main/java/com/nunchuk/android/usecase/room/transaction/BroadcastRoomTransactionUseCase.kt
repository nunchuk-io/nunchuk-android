package com.nunchuk.android.usecase.room.transaction

import com.nunchuk.android.model.NunchukMatrixEvent
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface BroadcastRoomTransactionUseCase {
    fun execute(initEventId: String): Flow<NunchukMatrixEvent>
}

internal class BroadcastRoomTransactionUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BroadcastRoomTransactionUseCase {

    override fun execute(initEventId: String) = flow {
        emit(nativeSdk.broadcastRoomTransaction(initEventId))
    }

}