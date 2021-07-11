package com.nunchuk.android.messages.usecase.contact

import com.nunchuk.android.messages.repository.ContactsRepository
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