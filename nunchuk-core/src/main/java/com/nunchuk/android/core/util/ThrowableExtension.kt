package com.nunchuk.android.core.util

import java.net.SocketTimeoutException
import java.net.UnknownHostException

val Throwable.isNoInternetException: Boolean
    get() = this is UnknownHostException || this is SocketTimeoutException