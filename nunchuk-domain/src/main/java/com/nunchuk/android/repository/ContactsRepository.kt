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