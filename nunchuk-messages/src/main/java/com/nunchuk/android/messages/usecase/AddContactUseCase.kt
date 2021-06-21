package com.nunchuk.android.messages.usecase

import com.nunchuk.android.messages.repository.ContactsRepository
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.BaseUseCase
import javax.inject.Inject

interface AddContactUseCase {
    suspend fun execute(friendId: String): Result<Unit>
}

internal class AddContactUseCaseImpl @Inject constructor(
    private val repository: ContactsRepository
) : BaseUseCase(), AddContactUseCase {

    override suspend fun execute(friendId: String) = exe {
        repository.addContacts(friendId)
    }

}