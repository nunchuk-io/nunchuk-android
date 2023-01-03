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

package com.nunchuk.android.utils

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toSignerModel
import com.nunchuk.android.model.Contact
import com.nunchuk.android.model.JoinKey


class TransactionException(message: String) : Exception(message)

// why not use name but email?
fun JoinKey.retrieveInfo(
    isUserKey: Boolean, walletSingers: List<SignerModel>, contacts: List<Contact>
) = if (isUserKey) retrieveByLocalKeys(walletSingers, contacts) else retrieveByContacts(contacts)

fun JoinKey.retrieveByLocalKeys(
    localSignedSigners: List<SignerModel>, contacts: List<Contact>
) = localSignedSigners.firstOrNull { it.fingerPrint == masterFingerprint } ?: retrieveByContacts(
    contacts
)

fun JoinKey.retrieveByContacts(contacts: List<Contact>) =
    copy(name = getDisplayName(contacts)).toSignerModel().copy(localKey = false)

fun JoinKey.getDisplayName(contacts: List<Contact>): String {
    contacts.firstOrNull { it.chatId == chatId }?.apply {
        if (email.isNotEmpty()) {
            return@getDisplayName email
        }
        if (name.isNotEmpty()) {
            return@getDisplayName chatId
        }
    }
    return chatId
}