package com.nunchuk.android.messages.usecase.contact

import android.annotation.SuppressLint
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.messages.model.Contact
import com.nunchuk.android.messages.repository.ContactsRepository
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

interface GetContactsUseCase {
    fun execute(): Flowable<List<Contact>>
}

internal class GetContactsUseCaseImpl @Inject constructor(
    private val accountManager: AccountManager,
    private val repository: ContactsRepository
) : GetContactsUseCase {

    @SuppressLint("CheckResult")
    override fun execute() = with(repository) {
        val email = accountManager.getAccount().email
        getRemoteContacts(email)
            .observeOn(Schedulers.io())
            .subscribeOn(Schedulers.io())
            .subscribe({}, {})
        getLocalContacts(email)
    }

}