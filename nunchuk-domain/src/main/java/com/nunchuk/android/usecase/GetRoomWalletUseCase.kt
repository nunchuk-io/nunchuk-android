package com.nunchuk.android.usecase

import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface GetRoomWalletUseCase {
    fun execute(roomId: String): Flow<RoomWallet>
}

internal class GetRoomWalletUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : GetRoomWalletUseCase {

    override fun execute(roomId: String) = flow {
        emit(
            nativeSdk.getRoomWallet(roomId = roomId)
        )
    }

}

interface GetAllRoomWalletsUseCase {
    fun execute(): Flow<List<RoomWallet>>
}

internal class GetAllRoomWalletsUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : GetAllRoomWalletsUseCase {
    override fun execute() = flow {
        emit(
            nativeSdk.getAllRoomWallets()
        )
    }

}