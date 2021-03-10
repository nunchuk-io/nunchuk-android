package com.nunchuk.android.exception

class NunchukException(override val code: Int) : BaseException(code) {

    companion object {
        const val INVALID_ADDRESS = -1001
        const val INVALID_AMOUNT = -1002
        const val INVALID_PSBT = -1003
        const val INVALID_RAW_TX = -1004
        const val INVALID_FEE_RATE = -1005
        const val INVALID_ADDRESS_TYPE = -1006
        const val INVALID_WALLET_TYPE = -1007
        const val AMOUNT_OUT_OF_RANGE = -1008
        const val RUN_OUT_OF_CACHED_XPUB = -1009
        const val MESSAGE_TOO_SHORT = -1010
        const val COIN_SELECTION_ERROR = -1011
        const val PSBT_INCOMPLETE = -1012
        const val SERVER_REQUEST_ERROR = -1013
        const val INVALID_PASSPHRASE = -1014
        const val PASSPHRASE_ALREADY_USED = -1015
        const val INVALID_CHAIN = -1016
        const val INVALID_PARAMETER = -1017
        const val CREATE_DUMMY_SIGNATURE_ERROR = -1018
        const val APP_RESTART_REQUIRED = -1019
        const val INVALID_FORMAT = -1020
    }

}

