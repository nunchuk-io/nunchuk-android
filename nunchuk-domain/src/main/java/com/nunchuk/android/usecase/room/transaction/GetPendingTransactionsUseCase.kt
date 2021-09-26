package com.nunchuk.android.usecase.room.transaction

import com.nunchuk.android.model.RoomTransaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface GetPendingTransactionsUseCase {
    fun execute(roomId: String): Flow<List<RoomTransaction>>
}

internal class GetPendingTransactionsUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : GetPendingTransactionsUseCase {

    override fun execute(roomId: String) = flow {
        emit(nativeSdk.getPendingTransactions(roomId))
    }

}

interface GetRoomTransactionUseCase {
    fun execute(initEventId: String): Flow<RoomTransaction>
}

internal class GetRoomTransactionUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : GetRoomTransactionUseCase {

    override fun execute(initEventId: String) = flow {
        emit(nativeSdk.getRoomTransaction(initEventId))
    }
}