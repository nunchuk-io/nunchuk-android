package com.nunchuk.android.transaction.components.details

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toSignerModel
import com.nunchuk.android.model.Contact
import com.nunchuk.android.model.JoinKey


class TransactionException(message: String) : Exception(message)

// why not use name but email?
internal fun JoinKey.retrieveInfo(
    isUserKey: Boolean, walletSingers: List<SignerModel>, contacts: List<Contact>
) = if (isUserKey) retrieveByLocalKeys(walletSingers, contacts) else retrieveByContacts(contacts)

internal fun JoinKey.retrieveByLocalKeys(
    localSignedSigners: List<SignerModel>, contacts: List<Contact>
) = localSignedSigners.firstOrNull { it.fingerPrint == masterFingerprint } ?: retrieveByContacts(
    contacts
)

internal fun JoinKey.retrieveByContacts(contacts: List<Contact>) =
    copy(name = getDisplayName(contacts)).toSignerModel().copy(localKey = false)

internal fun JoinKey.getDisplayName(contacts: List<Contact>): String {
    contacts.firstOrNull { it.chatId == chatId }?.apply {
        if (email.isNotEmpty()) {
            return@getDisplayName email
        }
        if (name.isNotEmpty()) {
            return@getDisplayName chatId
        }
    }
    return chatId
}