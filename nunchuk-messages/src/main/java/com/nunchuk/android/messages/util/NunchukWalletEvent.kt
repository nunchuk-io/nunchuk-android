package com.nunchuk.android.messages.util

internal const val KEY = "msgtype"

enum class WalletEventType(val type: String) {
    INIT("io.nunchuk.wallet.init"),
    JOIN("io.nunchuk.wallet.join"),
    CREATE("io.nunchuk.wallet.create"),
    LEAVE("io.nunchuk.wallet.leave"),
    CANCEL("io.nunchuk.wallet.cancel"),
    READY("io.nunchuk.wallet.ready");

    companion object {
        fun of(type: String): WalletEventType = values().firstOrNull { it.type == type } ?: throw IllegalArgumentException("Invalid wallet type $type")
    }

}

enum class TransactionEventType(val type: String) {
    INIT("io.nunchuk.transaction.init"),
    SIGN("io.nunchuk.transaction.sign"),
    REJECT("io.nunchuk.transaction.reject"),
    CANCEL("io.nunchuk.transaction.cancel"),
    READY("io.nunchuk.transaction.ready"),
    BROADCAST("io.nunchuk.transaction.broadcast");

    companion object {
        fun of(type: String) = values().firstOrNull { it.type == type } ?: throw IllegalArgumentException("Invalid transaction type $type")
    }
}
