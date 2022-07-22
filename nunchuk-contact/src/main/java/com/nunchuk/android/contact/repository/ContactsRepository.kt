package com.nunchuk.android.contact.repository

import com.nunchuk.android.contact.api.*
import com.nunchuk.android.contact.mapper.*
import com.nunchuk.android.model.Contact
import com.nunchuk.android.model.ReceiveContact
import com.nunchuk.android.model.SentContact
import com.nunchuk.android.persistence.dao.ContactDao
import com.nunchuk.android.persistence.entity.ContactEntity
import com.nunchuk.android.persistence.updateOrInsert
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface ContactsRepository {

    fun getLocalContacts(accountId: String): Flowable<List<Contact>>

    fun getRemoteContacts(accountId: String): Completable

    fun addContacts(emails: List<String>): Flow<List<String>>

    fun getPendingSentContacts(): Single<List<SentContact>>

    fun getPendingApprovalContacts(): Single<List<ReceiveContact>>

    fun acceptContact(contactId: String): Completable

    fun cancelContact(contactId: String): Completable

    fun searchContact(email: String): Flow<UserResponse>

    fun autoCompleteSearch(keyword: String): Flow<List<UserResponse>>

    fun updateContact(imageUrl: String): Flow<UserResponse>

    fun invite(friendEmails: List<String>): Flow<Unit>

}

internal class ContactsRepositoryImpl @Inject constructor(
    private val api: ContactApi,
    private val contactDao: ContactDao
) : ContactsRepository {

    override fun getLocalContacts(accountId: String) = contactDao.getContacts(accountId).map(List<ContactEntity>::toModels)

    override fun getRemoteContacts(accountId: String) = Completable.fromCallable {
        val response = api.getContacts().blockingGet()
        saveDatabase(accountId, response.data.users)
    }

    private fun saveDatabase(accountId: String, remoteContacts: List<UserResponse>) {
        val remoteContactIds = remoteContacts.map(UserResponse::id)
        val localContacts = contactDao.getContacts(accountId).blockingFirst()
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

    override fun getPendingSentContacts() = api.getPendingSentContacts().map { it.data.users.toSentContacts() }

    override fun getPendingApprovalContacts() = api.getPendingApprovalContacts().map { it.data.users.toReceiveContacts() }

    override fun acceptContact(contactId: String): Completable {
        val payload = AcceptRequestPayload(contactId)
        return api.acceptContact(payload)
    }

    override fun cancelContact(contactId: String): Completable {
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

    override fun invite(friendEmails: List<String>) = flow {
        val payload = InviteContactPayload(friendEmails)
        val response = api.invite(payload)
        emit(
            if (response.isSuccess) Unit else response.data
        )
    }

}

