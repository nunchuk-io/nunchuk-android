package com.nunchuk.android.messages.usecase.contact

import com.nunchuk.android.messages.model.SentContact
import com.nunchuk.android.messages.repository.ContactsRepository
import com.nunchuk.android.usecase.BaseUseCase
import io.reactivex.Single
import javax.inject.Inject

interface GetSentContactsUseCase {
    fun execute(): Single<List<SentContact>>
}

internal class GetSentContactsUseCaseImpl @Inject constructor(
    private val contactsRepository: ContactsRepository
) : BaseUseCase(), GetSentContactsUseCase {

    override fun execute() = contactsRepository.getPendingSentContacts()

}