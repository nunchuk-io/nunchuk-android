package com.nunchuk.android.contact.repository

import com.nunchuk.android.contact.api.*
import com.nunchuk.android.contact.mapper.*
import com.nunchuk.android.model.Contact
import com.nunchuk.android.model.ReceiveContact
import com.nunchuk.android.model.SentContact
import com.nunchuk.android.persistence.dao.ContactDao
import com.nunchuk.android.persistence.entity.ContactEntity
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import javax.inject.Inject

interface ContactsRepository {

    fun getLocalContacts(accountId: String): Flowable<List<Contact>>

    fun getRemoteContacts(accountId: String): Completable

    fun addContacts(emails: List<String>): Single<List<String>>

    fun getPendingSentContacts(): Single<List<SentContact>>

    fun getPendingApprovalContacts(): Single<List<ReceiveContact>>

    fun acceptContact(contactId: String): Completable

    fun cancelContact(contactId: String): Completable

    suspend fun searchContact(email: String): UserResponse

    suspend fun autoCompleteSearch(keyword: String): List<UserResponse>

}

internal class ContactsRepositoryImpl @Inject constructor(
    private val api: ContactApi,
    private val contactDao: ContactDao
) : ContactsRepository {

    override fun getLocalContacts(accountId: String) = contactDao.getContacts(accountId).map(List<ContactEntity>::toModels)

    override fun getRemoteContacts(accountId: String) = Completable.fromCallable {
        val response = api.getContacts().blockingGet()
        val items = response.data.users.toEntities(accountId)
        contactDao.insert(items)
    }

    override fun addContacts(emails: List<String>): Single<List<String>> {
        val payload = AddContactPayload(emails = emails)
        return api.addContacts(payload).map { it.data.failedEmails ?: emptyList() }
    }

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

    override suspend fun searchContact(email: String) = api.searchContact(email).data.user

    override suspend fun autoCompleteSearch(keyword: String): List<UserResponse> {
        val payload = AutoCompleteSearchContactPayload(keyword)
        return api.autoCompleteSearch(payload).data.users
    }

}

