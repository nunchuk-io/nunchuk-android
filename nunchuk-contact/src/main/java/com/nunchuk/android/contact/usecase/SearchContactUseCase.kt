package com.nunchuk.android.contact.usecase

import com.nunchuk.android.contact.api.UserResponse
import com.nunchuk.android.contact.repository.ContactsRepository
import com.nunchuk.android.usecase.BaseUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface SearchContactUseCase {
    fun execute(email: String): Flow<UserResponse>
}

internal class SearchContactUseCaseImpl @Inject constructor(
    private val contactsRepository: ContactsRepository
) : BaseUseCase(), SearchContactUseCase {

    override fun execute(email: String) = contactsRepository.searchContact(email)

}