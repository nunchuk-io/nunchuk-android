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

import com.nunchuk.android.core.guestmode.LastSignInModeHolder
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.util.AppUpdateStateHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    fun shouldShowOnboard(): Boolean?

    fun setShouldShowOnboard(shouldShow: Boolean)

    fun restoreAccountFromBackup()

    val accountInfoFlow: StateFlow<AccountInfo?>

    fun setLastDecoyPin(decoyPin: String)

    fun getLastDecoyPin(): String

    fun removeAccountBackup()
}

@Singleton
internal class AccountManagerImpl @Inject constructor(
    private val accountSharedPref: AccountSharedPref,
    private val lastSignInModeHolder: LastSignInModeHolder
) : AccountManager {
    private val shouldShowOnBoard = MutableStateFlow<Boolean?>(null)
    private val _accountInfoFlow = MutableStateFlow<AccountInfo?>(accountSharedPref.getAccountInfo())

    override val accountInfoFlow = _accountInfoFlow.asStateFlow()

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
        _accountInfoFlow.update { accountInfo }
        accountSharedPref.storeAccountInfo(accountInfo)
        lastSignInModeHolder.clear()
    }

    override fun storePrimaryKeyInfo(primaryKeyInfo: PrimaryKeyInfo) {
        val account = accountSharedPref.getAccountInfo()
        storeAccount(account.copy(primaryKeyInfo = primaryKeyInfo))
    }

    override suspend fun signOut() {
        if (getAccount().loginType == SignInMode.PRIMARY_KEY.value) {
            lastSignInModeHolder.setLastLoginDecoyPin(getAccount().decoyPin)
        }
        clearUserData()
    }

    override fun clearUserData() {
        _accountInfoFlow.update { null }
        AppUpdateStateHolder.reset()
        accountSharedPref.clearAccountInfo()
    }

    override fun loginType(): Int = accountSharedPref.getAccountInfo().loginType

    override fun shouldShowOnboard() = shouldShowOnBoard.value

    override fun setShouldShowOnboard(shouldShow: Boolean) {
        shouldShowOnBoard.value = shouldShow
    }

    override fun restoreAccountFromBackup() {
        accountSharedPref.restoreAccountFromBackup()
        _accountInfoFlow.update { accountSharedPref.getAccountInfo() }
    }

    override fun setLastDecoyPin(decoyPin: String) {
        accountSharedPref.setLastDecoyPin(decoyPin)
    }

    override fun getLastDecoyPin(): String {
        return accountSharedPref.getLastDecoyPin()
    }

    override fun removeAccountBackup() {
        accountSharedPref.removeAccountBackup()
    }
}