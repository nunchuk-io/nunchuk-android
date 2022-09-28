package com.nunchuk.android.usecase.room.transaction

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.RoomTransaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetPendingTransactionUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk
) : UseCase<GetPendingTransactionUseCase.Data, RoomTransaction>(dispatcher) {

    override suspend fun execute(parameters: Data): RoomTransaction {
        return nativeSdk.getPendingTransactions(parameters.roomId).first { it.txId == parameters.txId }
    }

    data class Data(val roomId: String, val txId: String)
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