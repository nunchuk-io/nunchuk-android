package com.nunchuk.android.exception

data class NCNativeException constructor(
    override val message: String
) : Exception(message)