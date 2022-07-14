package com.nunchuk.android.contact.usecase

import android.annotation.SuppressLint
import com.nunchuk.android.contact.repository.ContactsRepository
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.model.Contact
import com.nunchuk.android.share.GetContactsUseCase
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

internal class GetContactsUseCaseImpl @Inject constructor(
    private val accountManager: AccountManager,
    private val repository: ContactsRepository
) : GetContactsUseCase {

    @SuppressLint("CheckResult")
    override fun execute(): Flowable<List<Contact>> = with(repository) {
        val email = accountManager.getAccount().email
        getRemoteContacts(email)
            .observeOn(Schedulers.io())
            .subscribeOn(Schedulers.io())
            .toFlowable<Unit>()
            .onErrorReturn { }
            .flatMap { getLocalContacts(email) }
    }
}