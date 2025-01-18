package com.nunchuk.android.messages.usecase.message

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.FreeGroupMessage
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import javax.inject.Inject

class GetGroupMessageAccountUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk,
) : UseCase<Unit, List<RoomSummary>>(dispatcher) {
    override suspend fun execute(parameters: Unit): List<RoomSummary> {
        val groupWallets = nativeSdk.getGroupWallets()
        return supervisorScope {
            groupWallets.map { wallet ->
                async {
                    val message = nativeSdk.getGroupWalletMessages(wallet.id, 0, 10).firstOrNull()
                    message?.toRoomSummary(wallet) ?: RoomSummary(
                        roomId = wallet.id,
                        encryptionEventTs = 0,
                        isEncrypted = false,
                        typingUsers = emptyList(),
                    )
                }
            }.awaitAll()
        }
    }

    private fun FreeGroupMessage.toRoomSummary(wallet: Wallet): RoomSummary {
        return RoomSummary(
            roomId = this.id,
            encryptionEventTs = this.timestamp * 1000,
            isEncrypted = true,
            typingUsers = emptyList(),
            name = wallet.name,
        )
    }
}