package com.nunchuk.android.messages.usecase.contact

import com.nunchuk.android.messages.api.UserResponse
import com.nunchuk.android.messages.repository.ContactsRepository
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.BaseUseCase
import javax.inject.Inject

interface GetContactsUseCase {
    suspend fun execute(): Result<List<UserResponse>>
}

internal class GetContactsUseCaseImpl @Inject constructor(
    private val contactsRepository: ContactsRepository
) : BaseUseCase(), GetContactsUseCase {

    override suspend fun execute() = exe(contactsRepository::getContacts)

}