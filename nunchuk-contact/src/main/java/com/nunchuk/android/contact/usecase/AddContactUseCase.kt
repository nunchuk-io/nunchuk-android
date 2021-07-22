package com.nunchuk.android.contact.usecase

import com.nunchuk.android.contact.repository.ContactsRepository
import io.reactivex.Single
import javax.inject.Inject

interface AddContactUseCase {
    fun execute(emails: List<String>): Single<List<String>>
}

internal class AddContactUseCaseImpl @Inject constructor(
    private val repository: ContactsRepository
) : AddContactUseCase {

    override fun execute(emails: List<String>) = repository.addContacts(emails)

}