package com.nunchuk.android.core.constants

object Constants {
    const val MAIN_NET_HOST = "mainnet.nunchuk.io:51001"
    const val TEST_NET_HOST = "testnet.nunchuk.io:50001"
    const val SIG_NET_HOST = "signet.nunchuk.io:50002"

    const val TESTNET_URL_TEMPLATE = "https://mempool.space/testnet/tx/"
    const val MAINNET_URL_TEMPLATE = "https://mempool.space/tx/"
    const val GLOBAL_SIGNET_EXPLORER = "https://explorer.bc-2.jp"

}

enum class RoomAction {
    SEND, RECEIVE
}
