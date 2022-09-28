package com.nunchuk.android.contact.usecase

import com.nunchuk.android.model.UserResponse
import com.nunchuk.android.repository.ContactsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface AutoCompleteSearchUseCase {
    fun execute(keyword: String): Flow<List<UserResponse>>
}

internal class AutoCompleteSearchUseCaseImpl @Inject constructor(
    private val contactsRepository: ContactsRepository
) : AutoCompleteSearchUseCase {

    override fun execute(keyword: String) = contactsRepository.autoCompleteSearch(keyword)

}