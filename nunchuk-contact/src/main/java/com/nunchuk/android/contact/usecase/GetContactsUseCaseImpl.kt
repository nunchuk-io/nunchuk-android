package com.nunchuk.android.contact.usecase

import android.annotation.SuppressLint
import com.nunchuk.android.contact.repository.ContactsRepository
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Contact
import com.nunchuk.android.share.GetContactsUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class GetContactsUseCaseImpl @Inject constructor(
    private val accountManager: AccountManager,
    private val repository: ContactsRepository,
    private val appScope: CoroutineScope,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : GetContactsUseCase {

    @SuppressLint("CheckResult")
    override fun execute(): Flow<List<Contact>> = with(repository) {
        val email = accountManager.getAccount().email
        appScope.launch(dispatcher) {
            getRemoteContacts(email)
        }
        getLocalContacts(email).flowOn(dispatcher)
    }

}