package com.nunchuk.android.contact.usecase

import com.nunchuk.android.model.UserResponse
import com.nunchuk.android.repository.ContactsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface SearchContactUseCase {
    fun execute(email: String): Flow<UserResponse>
}

internal class SearchContactUseCaseImpl @Inject constructor(
    private val contactsRepository: ContactsRepository
) : SearchContactUseCase {

    override fun execute(email: String) = contactsRepository.searchContact(email)

}