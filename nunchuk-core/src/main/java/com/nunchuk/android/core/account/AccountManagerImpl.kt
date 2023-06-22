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

package com.nunchuk.android.core.account

import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.util.AppUpdateStateHolder
import javax.inject.Inject
import javax.inject.Singleton

interface AccountManager {
    fun isHasAccountBefore(): Boolean

    fun isAccountExisted(): Boolean

    fun isAccountActivated(): Boolean

    fun isStaySignedIn(): Boolean

    fun getAccount(): AccountInfo

    fun getPrimaryKeyInfo(): PrimaryKeyInfo?

    fun storeAccount(accountInfo: AccountInfo)

    fun storePrimaryKeyInfo(primaryKeyInfo: PrimaryKeyInfo)

    suspend fun signOut()

    fun clearUserData()

    fun loginType(): Int

    fun removeAccount()
}

@Singleton
internal class AccountManagerImpl @Inject constructor(
    private val accountSharedPref: AccountSharedPref,
) : AccountManager {

    override fun isHasAccountBefore(): Boolean = accountSharedPref.isHasAccountBefore()

    override fun isAccountExisted() = accountSharedPref.getAccountInfo().token.isNotBlank()

    override fun isAccountActivated() = accountSharedPref.getAccountInfo().activated

    override fun isStaySignedIn() = accountSharedPref.getAccountInfo().staySignedIn

    override fun getAccount() = accountSharedPref.getAccountInfo()

    override fun getPrimaryKeyInfo(): PrimaryKeyInfo? {
        val account = getAccount()
        if (account.loginType != SignInMode.PRIMARY_KEY.value) return null
        return account.primaryKeyInfo
    }

    override fun storeAccount(accountInfo: AccountInfo) {
        accountSharedPref.storeAccountInfo(accountInfo)
    }

    override fun storePrimaryKeyInfo(primaryKeyInfo: PrimaryKeyInfo) {
        val account = accountSharedPref.getAccountInfo()
        storeAccount(account.copy(primaryKeyInfo = primaryKeyInfo))
    }

    override suspend fun signOut() {
        clearUserData()
    }

    override fun clearUserData() {
        AppUpdateStateHolder.reset()
        accountSharedPref.clearAccountInfo()
    }

    override fun loginType(): Int = accountSharedPref.getAccountInfo().loginType

    override fun removeAccount() {
        accountSharedPref.removeAccount()
    }
}