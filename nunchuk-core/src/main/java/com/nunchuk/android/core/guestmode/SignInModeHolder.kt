package com.nunchuk.android.core.guestmode

import com.nunchuk.android.core.account.AccountManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignInModeHolder @Inject constructor(private val accountManager: AccountManager) {

    private var currentMode: SignInMode = SignInMode.UNKNOWN

    fun getCurrentMode(): SignInMode = currentMode

    fun setCurrentMode(mode: SignInMode) {
        currentMode = mode
        if (currentMode.isGuestMode().not()) {
            val loginType = accountManager.loginType()
            if (loginType != currentMode.value) {
                val account = accountManager.getAccount()
                accountManager.storeAccount(account.copy(loginType = currentMode.value))
            }
        }
    }

    fun clear() {
        currentMode = SignInMode.UNKNOWN
    }
}

enum class SignInMode(val value: Int) {
    UNKNOWN(-1), EMAIL(0), PRIMARY_KEY(1), GUEST_MODE(2);
}

fun SignInMode.isGuestMode() = this == SignInMode.GUEST_MODE
fun SignInMode.isPrimaryKey() = this == SignInMode.PRIMARY_KEY
fun SignInMode.isUnknown() = this == SignInMode.UNKNOWN