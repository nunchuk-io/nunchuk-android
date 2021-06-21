package com.nunchuk.android.messages.usecase

import com.nunchuk.android.messages.api.UserResponse
import com.nunchuk.android.messages.repository.ContactsRepository
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.BaseUseCase
import javax.inject.Inject

interface GetSentContactsUseCase {
    suspend fun execute(): Result<List<UserResponse>>
}

internal class GetSentContactsUseCaseImpl @Inject constructor(
    private val contactsRepository: ContactsRepository
) : BaseUseCase(), GetSentContactsUseCase {

    override suspend fun execute() = exe {
        contactsRepository.getPendingSentContacts()
    }
}