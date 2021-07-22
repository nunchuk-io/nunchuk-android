package com.nunchuk.android.contact.usecase

import com.nunchuk.android.model.ReceiveContact
import com.nunchuk.android.contact.repository.ContactsRepository
import io.reactivex.Single
import javax.inject.Inject

interface GetReceivedContactsUseCase {
    fun execute(): Single<List<ReceiveContact>>
}

internal class GetReceivedContactsUseCaseImpl @Inject constructor(
    private val contactsRepository: ContactsRepository
) : GetReceivedContactsUseCase {

    override fun execute() = contactsRepository.getPendingApprovalContacts()

}