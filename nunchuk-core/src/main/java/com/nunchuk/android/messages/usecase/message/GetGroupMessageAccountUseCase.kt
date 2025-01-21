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
import javax.inject.Inject

class GetGroupMessageAccountUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk,
) : UseCase<Unit, List<GroupWalletMessage>>(dispatcher) {
    override suspend fun execute(parameters: Unit): List<GroupWalletMessage> {
        val groupWallets = nativeSdk.getGroupWallets()
        val messages = supervisorScope {
            groupWallets.map { wallet ->
                async {
                    val message = nativeSdk.getGroupWalletMessages(wallet.id, 0, 10).firstOrNull()
                    message?.groupWalletMessage(wallet)
                }
            }.awaitAll()
        }
        return messages.filterNotNull()
    }

    private fun FreeGroupMessage.groupWalletMessage(wallet: Wallet): GroupWalletMessage {
        return GroupWalletMessage(
            id = id,
            content = content,
            timestamp = timestamp * 1000,
            walletName = wallet.name,
            walletId = wallet.id,
            numOfUsers = wallet.totalRequireSigns,
        )
    }
}

data class GroupWalletMessage(
    val id: String,
    val content: String,
    val timestamp: Long,
    val walletName: String,
    val walletId: String,
    val numOfUsers: Int = 0,
)