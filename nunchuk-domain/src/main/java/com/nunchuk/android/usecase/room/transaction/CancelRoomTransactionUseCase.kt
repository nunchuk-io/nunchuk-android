package com.nunchuk.android.usecase.room.transaction

import com.nunchuk.android.model.NunchukMatrixEvent
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface CancelRoomTransactionUseCase {
    fun execute(initEventId: String, reason: String): Flow<NunchukMatrixEvent>
}

internal class CancelRoomTransactionUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : CancelRoomTransactionUseCase {

    override fun execute(initEventId: String, reason: String) = flow {
        emit(nativeSdk.cancelRoomTransaction(initEventId, reason))
    }

}