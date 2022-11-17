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

package com.nunchuk.android.repository

import com.nunchuk.android.model.Contact
import com.nunchuk.android.model.ReceiveContact
import com.nunchuk.android.model.SentContact
import com.nunchuk.android.model.UserResponse
import kotlinx.coroutines.flow.Flow

interface ContactsRepository {

    fun getLocalContacts(accountId: String): Flow<List<Contact>>

    suspend fun getRemoteContacts(accountId: String)

    fun addContacts(emails: List<String>): Flow<List<String>>

    suspend fun getPendingSentContacts(): List<SentContact>

    suspend fun getPendingApprovalContacts(): List<ReceiveContact>

    suspend fun acceptContact(contactId: String)

    suspend fun cancelContact(contactId: String)

    suspend fun getContact(chatId: String) : Contact?

    fun searchContact(email: String): Flow<UserResponse>

    fun autoCompleteSearch(keyword: String): Flow<List<UserResponse>>

    fun updateContact(imageUrl: String): Flow<UserResponse>

    fun invite(friendEmails: List<String>): Flow<Unit>

}