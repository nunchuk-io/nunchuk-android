package com.nunchuk.android.usecase.contact

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Contact
import com.nunchuk.android.repository.ContactsRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetContactByChatIdUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val repository: ContactsRepository
) : UseCase<String, Contact?>(dispatcher) {
    override suspend fun execute(parameters: String): Contact? {
        return repository.getContact(parameters)
    }
}