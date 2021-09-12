package com.nunchuk.android.usecase

import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import timber.log.Timber
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
    }.catch {
        Timber.e("Get all room wallets error ", it)
        emit(emptyList())
    }

}