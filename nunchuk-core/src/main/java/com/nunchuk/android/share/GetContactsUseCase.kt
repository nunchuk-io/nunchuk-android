package com.nunchuk.android.share

import com.nunchuk.android.model.Contact
import kotlinx.coroutines.flow.Flow

interface GetContactsUseCase {
    fun execute(): Flow<List<Contact>>
}