package com.nunchuk.android.contact.usecase

import com.nunchuk.android.repository.ContactsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface InviteFriendUseCase {
    fun execute(emails: List<String>): Flow<Unit>
}

internal class InviteFriendUseCaseImpl @Inject constructor(
    private val repository: ContactsRepository
) : InviteFriendUseCase {

    override fun execute(emails: List<String>) = repository.invite(emails).flowOn(Dispatchers.IO)

}