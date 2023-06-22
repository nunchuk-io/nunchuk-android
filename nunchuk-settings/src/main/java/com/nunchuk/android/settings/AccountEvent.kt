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

package com.nunchuk.android.settings

import com.nunchuk.android.core.account.AccountInfo

data class AccountState(
    val account: AccountInfo = AccountInfo(),
    val syncProgress: Int = 0,
    val finishedSync: Boolean = false,
    val localCurrency: String = ""
) {
    fun isSyncing() = syncProgress in 1..99
}

sealed class AccountEvent {
    object SignOutEvent : AccountEvent()
    data class GetUserProfileSuccessEvent(val name: String? = null, val avatarUrl: String? = null) : AccountEvent()
    data class UploadPhotoSuccessEvent(val matrixUri: String? = null) : AccountEvent()
    data class LoadingEvent(val loading: Boolean = false) : AccountEvent()

    data class ShowError(val message: String? = null) : AccountEvent()
}