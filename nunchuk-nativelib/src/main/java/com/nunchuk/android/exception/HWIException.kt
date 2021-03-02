package com.nunchuk.android.exception

class HWIException(override val code: Int) : BaseException(code) {

    companion object {
        const val NO_DEVICE_TYPE = -4001
        const val MISSING_ARGUMENTS = -4002
        const val DEVICE_CONN_ERROR = -4003
        const val UNKNOWN_DEVICE_TYPE = -4004
        const val INVALID_TX = -4005
        const val NO_PASSWORD = -4006
        const val BAD_ARGUMENT = -4007
        const val NOT_IMPLEMENTED = -4008
        const val UNAVAILABLE_ACTION = -4009
        const val DEVICE_ALREADY_INIT = -4010
        const val DEVICE_ALREADY_UNLOCKED = -4011
        const val DEVICE_NOT_READY = -4012
        const val UNKNOWN_ERROR = -4013
        const val ACTION_CANCELED = -4014
        const val DEVICE_BUSY = -4015
        const val NEED_TO_BE_ROOT = -4016
        const val HELP_TEXT = -4017
        const val DEVICE_NOT_INITIALIZED = -4018

        // Nunchuk-defined error codes
        const val RUN_ERROR = -4099
        const val INVALID_RESULT = -4098
    }

}