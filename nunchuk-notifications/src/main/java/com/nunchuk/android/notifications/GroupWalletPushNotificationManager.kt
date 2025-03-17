package com.nunchuk.android.notifications

import com.nunchuk.android.usecase.free.groupwallet.DecryptGroupTxIdUseCase
import com.nunchuk.android.usecase.free.groupwallet.DecryptGroupWalletIdUseCase
import timber.log.Timber
import javax.inject.Inject

class GroupWalletPushNotificationManager @Inject constructor(
    private val intentProvider: PushNotificationIntentProvider,
    val decryptGroupWalletIdUseCase: DecryptGroupWalletIdUseCase,
    val decryptGroupTxIdUseCase: DecryptGroupTxIdUseCase
) {

    companion object {
        private const val GROUP_WALLET = "group_wallet"

        interface TYPE {
            companion object {
                const val GROUP_WALLET_CHAT = "group_wallet.chat"
                const val GROUP_WALLET_FINALIZE = "group_wallet.finalize"
                const val GROUP_WALLET_TRANSACTION_UPDATED = "group_wallet.transaction_updated"
            }
        }
    }

    suspend fun parseNotification(data: Map<String, String>): PushNotificationData? {
        val type = data["type"]
        if (type?.startsWith(GROUP_WALLET) == false) return null
        val walletIdData = data["wallet_id"] ?: return null
        if (walletIdData.isEmpty()) return null
        val walletId = decryptGroupWalletIdUseCase(DecryptGroupWalletIdUseCase.Param(walletIdData)).getOrNull() ?: return null
        return when (type) {
            TYPE.GROUP_WALLET_CHAT -> {
                PushNotificationData(
                    title = data["title"] ?: "",
                    message = data["message"] ?: "",
                    id = System.currentTimeMillis(),
                    intent = intentProvider.getFreeGroupWalletChatIntent(walletId)
                )
            }

            TYPE.GROUP_WALLET_FINALIZE -> {
                PushNotificationData(
                    title = data["title"] ?: "",
                    message = data["message"] ?: "",
                    id = System.currentTimeMillis(),
                    intent = intentProvider.getWalletDetailIntent(walletId)
                )
            }

            TYPE.GROUP_WALLET_TRANSACTION_UPDATED -> {
                val txIdData = data["transaction_id"] ?: return null
                if (txIdData.isEmpty()) return null
                Timber.tag("notification-service-fcm").e("txIdData: $txIdData")
                var txId = ""
                runCatching {
                    txId = decryptGroupTxIdUseCase(DecryptGroupTxIdUseCase.Param(walletId, txIdData)).getOrNull().orEmpty()
                }.onSuccess {
                    Timber.tag("notification-service-fcm").e("txId: $txId")
                }.onFailure {
                    Timber.tag("notification-service-fcm").e(it)
                }
                PushNotificationData(
                    title = data["title"] ?: "",
                    message = data["message"] ?: "",
                    id = System.currentTimeMillis(),
                    intent = if (txId.isNotEmpty()) intentProvider.getTransactionDetailIntent(walletId = walletId, txId) else intentProvider.getWalletDetailIntent(walletId)
                )
            }

            else -> {
                null
            }
        }
    }
}