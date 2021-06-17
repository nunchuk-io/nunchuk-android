package com.nunchuk.android.messages.repository

import com.nunchuk.android.messages.api.AddContactPayload
import com.nunchuk.android.messages.api.UserApi
import javax.inject.Inject

interface ContactsRepository {
    suspend fun addContacts(emails: List<String>)
}

internal class ContactsRepositoryImpl @Inject constructor(
    private val api: UserApi
) : ContactsRepository {

    override suspend fun addContacts(emails: List<String>) {
        val payload = AddContactPayload(emails = emails)
        api.addContacts(payload)
    }

}
