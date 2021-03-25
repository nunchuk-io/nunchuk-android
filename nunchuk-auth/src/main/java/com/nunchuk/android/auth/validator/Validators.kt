package com.nunchuk.android.auth.validator

import java.util.regex.Pattern
import javax.inject.Inject

interface EmailValidator {
    fun valid(email: String): Boolean
}

internal class EmailValidatorImpl @Inject constructor() : EmailValidator {

    override fun valid(email: String) = Pattern.compile(
        "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                "\\@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(" +
                "\\." +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                ")+"
    ).matcher(email).matches()

}

interface NameValidator {
    fun valid(name: String): Boolean
}

internal class NameValidatorImpl @Inject constructor() : NameValidator {
    override fun valid(name: String) = name.isNotBlank()
}

internal fun doAfterValidate(result: Boolean = true, func: () -> Unit = {}): Boolean {
    func()
    return result
}