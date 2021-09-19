package com.nunchuk.android.contact.usecase

import com.nunchuk.android.contact.api.UserResponse
import com.nunchuk.android.contact.repository.ContactsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface UpdateAvatarUseCase {
    fun execute(imageUrl: String): Flow<UserResponse>
}

internal class UpdateAvatarUseCaseImpl @Inject constructor(
    private val repository: ContactsRepository
) : UpdateAvatarUseCase {

    override fun execute(imageUrl: String) = repository.updateContact(imageUrl).flowOn(Dispatchers.IO)

}