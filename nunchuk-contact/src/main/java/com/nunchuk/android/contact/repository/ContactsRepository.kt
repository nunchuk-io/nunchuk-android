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

package com.nunchuk.android.contact.repository

import com.nunchuk.android.contact.api.*
import com.nunchuk.android.contact.mapper.*
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.model.Contact
import com.nunchuk.android.model.ReceiveContact
import com.nunchuk.android.model.SentContact
import com.nunchuk.android.model.UserResponse
import com.nunchuk.android.persistence.dao.ContactDao
import com.nunchuk.android.persistence.entity.ContactEntity
import com.nunchuk.android.persistence.updateOrInsert
import com.nunchuk.android.repository.ContactsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class ContactsRepositoryImpl @Inject constructor(
    private val api: ContactApi,
    private val contactDao: ContactDao,
    private val accountManager: AccountManager
) : ContactsRepository {

    override fun getLocalContacts(accountId: String) = contactDao.getContacts(accountId).map(List<ContactEntity>::toModels)

    override suspend fun getRemoteContacts(accountId: String) {
        val response = api.getContacts()
        saveDatabase(accountId, response.data.users)
    }

    private suspend fun saveDatabase(accountId: String, remoteContacts: List<UserResponse>) {
        val remoteContactIds = remoteContacts.map(UserResponse::id)
        val localContacts = contactDao.getContacts(accountId).first()
        val localContactIds = localContacts.map(ContactEntity::id)
        val oldContactIds = localContacts.filterNot { remoteContactIds.contains(it.id) }.map(ContactEntity::id)
        val newContacts = remoteContacts.filterNot { localContactIds.contains(it.id) }
        storeContacts(accountId, newContacts)
        deleteOutdated(accountId, oldContactIds)
    }

    private fun storeContacts(accountId: String, contacts: List<UserResponse>) {
        if (contacts.isNotEmpty()) {
            contactDao.updateOrInsert(contacts.toEntities(accountId))
        }
    }

    private fun deleteOutdated(accountId: String, contactIds: List<String>) {
        if (contactIds.isNotEmpty()) {
            contactDao.deleteItems(accountId, contactIds)
        }
    }

    override fun addContacts(emails: List<String>): Flow<List<String>> = flow {
        val payload = AddContactPayload(emails = emails)
        emit(api.addContacts(payload))
    }.map { it.data.failedEmails ?: emptyList() }

    override suspend fun getPendingSentContacts(): List<SentContact> {
        val result = api.getPendingSentContacts()
        return result.data.users.toSentContacts()
    }

    override suspend fun getPendingApprovalContacts(): List<ReceiveContact> {
        val result = api.getPendingApprovalContacts()
        return result.data.users.toReceiveContacts()
    }

    override suspend fun acceptContact(contactId: String) {
        val payload = AcceptRequestPayload(contactId)
        return api.acceptContact(payload)
    }

    override suspend fun cancelContact(contactId: String) {
        val payload = CancelRequestPayload(contactId)
        return api.cancelRequest(payload)
    }

    override fun searchContact(email: String) = flow {
        emit(
            api.searchContact(email).data.user
        )
    }

    override fun autoCompleteSearch(keyword: String) = flow {
        val payload = AutoCompleteSearchContactPayload(keyword)
        emit(
            api.autoCompleteSearch(payload).data.users
        )
    }

    override fun updateContact(imageUrl: String): Flow<UserResponse> = flow {
        val payload = UpdateContactPayload(imageUrl)
        emit(
            api.updateContact(payload).data.user
        )
    }

    override suspend fun getContact(chatId: String) : Contact? {
        return contactDao.getContact(accountManager.getAccount().email, chatId)?.toModel()
    }

    override fun invite(friendEmails: List<String>) = flow {
        val payload = InviteContactPayload(friendEmails)
        val response = api.invite(payload)
        emit(
            if (response.isSuccess) Unit else response.data
        )
    }

}

