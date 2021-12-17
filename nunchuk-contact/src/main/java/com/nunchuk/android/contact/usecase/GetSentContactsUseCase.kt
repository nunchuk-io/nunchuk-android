package com.nunchuk.android.contact.usecase

import com.nunchuk.android.contact.repository.ContactsRepository
import com.nunchuk.android.model.SentContact
import io.reactivex.Single
import javax.inject.Inject

interface GetSentContactsUseCase {
    fun execute(): Single<List<SentContact>>
}

internal class GetSentContactsUseCaseImpl @Inject constructor(
    private val contactsRepository: ContactsRepository
) : GetSentContactsUseCase {

    override fun execute() = contactsRepository.getPendingSentContacts()

}