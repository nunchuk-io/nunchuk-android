package com.nunchuk.android.messages.repository

import com.nunchuk.android.messages.api.AddContactPayload
import com.nunchuk.android.messages.api.AutoCompleteSearchContactPayload
import com.nunchuk.android.messages.api.UserApi
import com.nunchuk.android.messages.api.UserResponse
import javax.inject.Inject

interface ContactsRepository {

    suspend fun getContacts(): List<UserResponse>

    suspend fun addContacts(friendId: String)

    suspend fun searchContact(email: String): UserResponse

    suspend fun getPendingSentContacts(): List<UserResponse>

    suspend fun getPendingApprovalContacts(): List<UserResponse>

    suspend fun autoCompleteSearch(keyword: String): List<UserResponse>

}

internal class ContactsRepositoryImpl @Inject constructor(
    private val api: UserApi
) : ContactsRepository {

    override suspend fun getContacts() = api.getContacts().data

    override suspend fun addContacts(friendId: String) {
        val payload = AddContactPayload(friend_id = friendId)
        api.addContacts(payload)
    }

    override suspend fun searchContact(email: String) = api.searchContact(email).data.user

    override suspend fun getPendingSentContacts() = api.getPendingSentContacts().data

    override suspend fun getPendingApprovalContacts() = api.getPendingApprovalContacts().data

    override suspend fun autoCompleteSearch(keyword: String): List<UserResponse> {
        val payload = AutoCompleteSearchContactPayload(keyword)
        return api.autoCompleteSearch(payload).data.users
    }

}
