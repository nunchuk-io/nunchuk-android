package com.nunchuk.android.messages.usecase.contact

import com.nunchuk.android.messages.api.UserResponse
import com.nunchuk.android.messages.repository.ContactsRepository
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.BaseUseCase
import javax.inject.Inject

interface GetReceivedContactsUseCase {
    suspend fun execute(): Result<List<UserResponse>>
}

internal class GetReceivedContactsUseCaseImpl @Inject constructor(
    private val contactsRepository: ContactsRepository
) : BaseUseCase(), GetReceivedContactsUseCase {

    override suspend fun execute() = exe(contactsRepository::getPendingApprovalContacts)

}