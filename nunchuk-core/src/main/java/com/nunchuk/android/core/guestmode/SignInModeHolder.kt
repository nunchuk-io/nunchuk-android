/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
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

import com.nunchuk.android.core.account.AccountInfo
import com.nunchuk.android.core.account.AccountManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignInModeHolder @Inject constructor(
    private val accountManager: AccountManager,
    applicationScope: CoroutineScope
) {

    private var currentMode: SignInMode = SignInMode.UNKNOWN

    init {
        getMode(accountManager.getAccount())
        applicationScope.launch {
            accountManager.accountInfoFlow.collectLatest { account ->
                getMode(account)
            }
        }
    }

    private fun getMode(account: AccountInfo?) {
        val isAccountExist = account != null && account.token.isNotEmpty()
        val loginType = account?.loginType ?: SignInMode.UNKNOWN.value
        if (isAccountExist) {
            when (loginType) {
                SignInMode.EMAIL.value -> {
                    setCurrentMode(SignInMode.EMAIL)
                }

                SignInMode.PRIMARY_KEY.value -> {
                    setCurrentMode(SignInMode.PRIMARY_KEY)
                }

                SignInMode.GUEST_MODE.value -> {
                    setCurrentMode(SignInMode.GUEST_MODE)
                }
            }
        } else if (loginType == SignInMode.GUEST_MODE.value) {
            setCurrentMode(SignInMode.GUEST_MODE)
        }
    }

    fun getCurrentMode(): SignInMode = currentMode

    private fun setCurrentMode(mode: SignInMode) {
        Timber.d("setCurrentMode: $mode")
        currentMode = mode
    }

    fun clear() {
        currentMode = SignInMode.UNKNOWN
    }
}

enum class SignInMode(val value: Int) {
    UNKNOWN(-1), EMAIL(0), PRIMARY_KEY(1), GUEST_MODE(2), OPENID(3);
}

fun SignInMode.isGuestMode() = this == SignInMode.GUEST_MODE
fun SignInMode.isPrimaryKey() = this == SignInMode.PRIMARY_KEY
