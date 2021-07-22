package com.nunchuk.android.share

import com.nunchuk.android.model.Contact
import io.reactivex.Flowable

interface GetContactsUseCase {
    fun execute(): Flowable<List<Contact>>
}