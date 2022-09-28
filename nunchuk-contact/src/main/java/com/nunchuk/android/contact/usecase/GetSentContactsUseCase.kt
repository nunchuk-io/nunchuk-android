package com.nunchuk.android.contact.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SentContact
import com.nunchuk.android.repository.ContactsRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetSentContactsUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : UseCase<Unit, List<SentContact>>(dispatcher) {

    override suspend fun execute(parameters: Unit): List<SentContact> {
        return contactsRepository.getPendingSentContacts()
    }
}