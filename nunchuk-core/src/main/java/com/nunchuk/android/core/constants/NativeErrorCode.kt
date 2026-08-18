package com.nunchuk.android.core.constants

object NativeErrorCode {
    const val INVALID_FEE_RATE = -1005
    const val COIN_SELECTION_ERROR = -1011
    const val SIGNER_EXISTS = -2009
    const val GROUP_WALLET_JOINED = -7012
    const val INVALID_SIGNED_MESSAGE = -1017

    // Jade (JadeException in the native SDK)
    const val JADE_QR_PIN_UNLOCK = -8000
    const val JADE_INVALID_PARAMETER = -8001
    const val JADE_SERVER_REQUEST_ERROR = -8002
}