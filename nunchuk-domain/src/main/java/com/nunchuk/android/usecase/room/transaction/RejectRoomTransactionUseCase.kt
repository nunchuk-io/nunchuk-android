package com.nunchuk.android.usecase.room.transaction

import com.nunchuk.android.model.NunchukMatrixEvent
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface RejectRoomTransactionUseCase {
    fun execute(initEventId: String, reason: String = ""): Flow<NunchukMatrixEvent>
}

internal class RejectRoomTransactionUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : RejectRoomTransactionUseCase {

    override fun execute(initEventId: String, reason: String) = flow {
        emit(nativeSdk.rejectRoomTransaction(initEventId, reason))
    }

}