package com.nunchuk.android.auth.validator

import javax.inject.Inject

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