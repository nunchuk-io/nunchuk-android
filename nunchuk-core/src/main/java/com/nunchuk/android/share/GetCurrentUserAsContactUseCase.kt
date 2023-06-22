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

package com.nunchuk.android.share

import com.nunchuk.android.core.account.AccountInfo
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.guestmode.isGuestMode
import com.nunchuk.android.model.Contact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface GetCurrentUserAsContactUseCase {
    fun execute(): Flow<Contact?>
}

internal class GetCurrentUserAsContactUseCaseImpl @Inject constructor(
    private val accountManager: AccountManager,
    private val signInModeHolder: SignInModeHolder
) : GetCurrentUserAsContactUseCase {

    override fun execute(): Flow<Contact?> = flow {
        emit(getCurrentAsContact())
    }

    private fun getCurrentAsContact() =
        if (signInModeHolder.getCurrentMode().isGuestMode()) null else accountManager.getAccount()
            .toContact()

}

private fun AccountInfo.toContact(): Contact = Contact(
    id = chatId,
    name = name,
    email = email,
    gender = "",
    avatar = avatarUrl.orEmpty(),
    status = "",
    chatId = chatId,
    loginType = getLoginType(loginType),
    username = username
)

private fun getLoginType(loginType: Int): String {
    if (loginType == SignInMode.PRIMARY_KEY.value) return Contact.PRIMARY_KEY
    if (loginType == SignInMode.EMAIL.value) return Contact.EMAIL
    return Contact.UNKNOWN
}
