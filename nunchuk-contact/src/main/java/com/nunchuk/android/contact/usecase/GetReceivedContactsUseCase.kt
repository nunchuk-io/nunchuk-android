package com.nunchuk.android.contact.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.ReceiveContact
import com.nunchuk.android.repository.ContactsRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetReceivedContactsUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : UseCase<Unit, List<ReceiveContact>>(dispatcher) {

    override suspend fun execute(parameters: Unit): List<ReceiveContact> {
        return contactsRepository.getPendingApprovalContacts()
    }
}