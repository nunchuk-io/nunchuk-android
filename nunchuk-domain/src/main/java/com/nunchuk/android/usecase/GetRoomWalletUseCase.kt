package com.nunchuk.android.usecase

import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface GetRoomWalletUseCase {
    fun execute(roomId: String): Flow<RoomWallet?>
}

internal class GetRoomWalletUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : GetRoomWalletUseCase {

    override fun execute(roomId: String) = flow {
        emit(
            if (nativeSdk.hasRoomWallet(roomId)) nativeSdk.getRoomWallet(roomId = roomId) else null
        )
    }.flowOn(Dispatchers.IO)

}

interface GetAllRoomWalletsUseCase {
    fun execute(): Flow<List<RoomWallet>>
}

internal class GetAllRoomWalletsUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : GetAllRoomWalletsUseCase {
    override fun execute() = flow {
        emit(nativeSdk.getAllRoomWallets())
    }.catch {
        emit(emptyList())
    }.flowOn(Dispatchers.IO)

}