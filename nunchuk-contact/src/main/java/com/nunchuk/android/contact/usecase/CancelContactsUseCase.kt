package com.nunchuk.android.contact.usecase

import com.nunchuk.android.contact.repository.ContactsRepository
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CancelContactsUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : UseCase<String, Unit>(dispatcher) {

    override suspend fun execute(parameters: String) {
        return contactsRepository.cancelContact(parameters)
    }
}