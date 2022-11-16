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

package com.nunchuk.android.core.account

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import javax.inject.Inject

internal class AccountSharedPref @Inject constructor(
    context: Context,
    private val gson: Gson
) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(ACCOUNT_PREFERENCE, Context.MODE_PRIVATE)

    fun getAccountInfo(): AccountInfo {
        val accountJson = sharedPreferences.getString(ACCOUNT_KEY, null)
        return if (accountJson.isNullOrEmpty()) {
            AccountInfo()
        } else {
            gson.fromJson(accountJson, AccountInfo::class.java)
        }
    }

    fun storeAccountInfo(accountInfo: AccountInfo) {
        sharedPreferences.edit().putString(ACCOUNT_KEY, gson.toJson(accountInfo)).apply()
    }

    fun clearAccountInfo() {
        sharedPreferences.edit().putString(ACCOUNT_KEY, gson.toJson(AccountInfo())).apply()
    }

    fun isHasAccountBefore() = sharedPreferences.contains(ACCOUNT_KEY)

    companion object {
        private const val ACCOUNT_PREFERENCE = "ACCOUNT_PREFERENCE"
        private const val ACCOUNT_KEY = "ACCOUNT_KEY"
    }
}