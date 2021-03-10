package com.nunchuk.android.exception

class RPCException(override val code: Int) : BaseException(code) {

    companion object {
        const val RPC_MISC_ERROR = -3001
        const val RPC_TYPE_ERROR = -3003
        const val RPC_WALLET_EXISTS = -3004
        const val RPC_INVALID_ADDRESS_OR_KEY = -3005
        const val RPC_OUT_OF_MEMORY = -3007
        const val RPC_INVALID_PARAMETER = -3008
        const val RPC_WALLET_NOT_FOUND = -3018
        const val RPC_DATABASE_ERROR = -3020
        const val RPC_DESERIALIZATION_ERROR = -3022
        const val RPC_VERIFY_ERROR = -3025
        const val RPC_VERIFY_REJECTED = -3026
        const val RPC_VERIFY_ALREADY_IN_CHAIN = -3027
        const val RPC_IN_WARMUP = -3028
        const val RPC_METHOD_DEPRECATED = -3032
        // Nunchuk-defined error codes
        const val RPC_REQUEST_ERROR = -3099
    }

}