/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

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