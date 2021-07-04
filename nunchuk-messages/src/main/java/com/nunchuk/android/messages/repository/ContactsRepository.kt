package com.nunchuk.android.messages.repository

import com.nunchuk.android.messages.api.AddContactPayload
import com.nunchuk.android.messages.api.AutoCompleteSearchContactPayload
import com.nunchuk.android.messages.api.ContactApi
import com.nunchuk.android.messages.api.UserResponse
import com.nunchuk.android.messages.mapper.toEntities
import com.nunchuk.android.messages.mapper.toModels
import com.nunchuk.android.messages.model.Contact
import com.nunchuk.android.persistence.dao.ContactDao
import com.nunchuk.android.persistence.entity.ContactEntity
import io.reactivex.Completable
import io.reactivex.Flowable
import javax.inject.Inject

interface ContactsRepository {

    fun getLocalContacts(): Flowable<List<Contact>>

    fun getRemoteContacts(): Completable

    suspend fun addContacts(emails: List<String>)

    suspend fun searchContact(email: String): UserResponse

    suspend fun getPendingSentContacts(): List<UserResponse>

    suspend fun getPendingApprovalContacts(): List<UserResponse>

    suspend fun autoCompleteSearch(keyword: String): List<UserResponse>

}

internal class ContactsRepositoryImpl @Inject constructor(
    private val api: ContactApi,
    private val contactDao: ContactDao
) : ContactsRepository {

    override fun getLocalContacts() = contactDao.getContacts().map(List<ContactEntity>::toModels)

    override fun getRemoteContacts(): Completable = Completable.fromCallable {
        val response = api.getContacts().blockingGet()
        val items = response.data.users.toEntities()
        contactDao.insert(items)
    }

    override suspend fun addContacts(emails: List<String>) {
        val payload = AddContactPayload(emails = emails)
        api.addContacts(payload)
    }

    override suspend fun searchContact(email: String) = api.searchContact(email).data.user

    override suspend fun getPendingSentContacts() = api.getPendingSentContacts().data.users

    override suspend fun getPendingApprovalContacts() = api.getPendingApprovalContacts().data.users

    override suspend fun autoCompleteSearch(keyword: String): List<UserResponse> {
        val payload = AutoCompleteSearchContactPayload(keyword)
        return api.autoCompleteSearch(payload).data.users
    }

}

