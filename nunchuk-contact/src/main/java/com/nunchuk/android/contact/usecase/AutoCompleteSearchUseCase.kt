package com.nunchuk.android.contact.usecase

import com.nunchuk.android.contact.api.UserResponse
import com.nunchuk.android.contact.repository.ContactsRepository
import com.nunchuk.android.usecase.BaseUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface AutoCompleteSearchUseCase {
    suspend fun execute(keyword: String): Flow<List<UserResponse>>
}

internal class AutoCompleteSearchUseCaseImpl @Inject constructor(
    private val contactsRepository: ContactsRepository
) : BaseUseCase(), AutoCompleteSearchUseCase {

    override suspend fun execute(keyword: String) = contactsRepository.autoCompleteSearch(keyword)

}