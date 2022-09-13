package com.nunchuk.android.contact.usecase

import com.nunchuk.android.repository.ContactsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface AddContactUseCase {
    fun execute(emails: List<String>): Flow<List<String>>
}

internal class AddContactUseCaseImpl @Inject constructor(
    private val repository: ContactsRepository
) : AddContactUseCase {

    override fun execute(emails: List<String>) = repository.addContacts(emails).flowOn(Dispatchers.IO)

}