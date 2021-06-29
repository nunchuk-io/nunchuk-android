package com.nunchuk.android.messages.usecase.contact

import com.nunchuk.android.messages.api.UserResponse
import com.nunchuk.android.messages.repository.ContactsRepository
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.BaseUseCase
import javax.inject.Inject

interface AutoCompleteSearchUseCase {
    suspend fun execute(keyword: String): Result<List<UserResponse>>
}

internal class AutoCompleteSearchUseCaseImpl @Inject constructor(
    private val contactsRepository: ContactsRepository
) : BaseUseCase(), AutoCompleteSearchUseCase {

    override suspend fun execute(keyword: String) = exe {
        contactsRepository.autoCompleteSearch(keyword)
    }

}