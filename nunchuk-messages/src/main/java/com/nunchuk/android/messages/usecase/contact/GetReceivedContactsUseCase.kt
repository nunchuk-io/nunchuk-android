package com.nunchuk.android.messages.usecase.contact

import com.nunchuk.android.messages.model.ReceiveContact
import com.nunchuk.android.messages.repository.ContactsRepository
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