package com.nunchuk.android.auth.exception

import com.nunchuk.android.exception.BaseException

abstract class AuthException(code: Int = 0) : BaseException(code)

internal class AccountExistedException : AuthException()

internal class AccountNotActivatedException : AuthException()

internal class AccountExpiredException : AuthException()