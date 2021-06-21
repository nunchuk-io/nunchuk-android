package com.nunchuk.android.messages.usecase

import com.nunchuk.android.messages.api.UserResponse
import com.nunchuk.android.messages.repository.ContactsRepository
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.BaseUseCase
import javax.inject.Inject

interface SearchContactUseCase {
    suspend fun execute(email: String): Result<UserResponse>
}

internal class SearchContactUseCaseImpl @Inject constructor(
    private val contactsRepository: ContactsRepository
) : BaseUseCase(), SearchContactUseCase {

    override suspend fun execute(email: String) = exe {
        contactsRepository.searchContact(email)
    }

}