package com.nunchuk.android.contact.usecase

import com.nunchuk.android.contact.api.UserResponse
import com.nunchuk.android.contact.repository.ContactsRepository
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