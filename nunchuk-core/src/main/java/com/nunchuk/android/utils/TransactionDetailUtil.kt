package com.nunchuk.android.utils

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toSignerModel
import com.nunchuk.android.model.Contact
import com.nunchuk.android.model.JoinKey


class TransactionException(message: String) : Exception(message)

// why not use name but email?
fun JoinKey.retrieveInfo(
    isUserKey: Boolean, walletSingers: List<SignerModel>, contacts: List<Contact>
) = if (isUserKey) retrieveByLocalKeys(walletSingers, contacts) else retrieveByContacts(contacts)

fun JoinKey.retrieveByLocalKeys(
    localSignedSigners: List<SignerModel>, contacts: List<Contact>
) = localSignedSigners.firstOrNull { it.fingerPrint == masterFingerprint } ?: retrieveByContacts(
    contacts
)

fun JoinKey.retrieveByContacts(contacts: List<Contact>) =
    copy(name = getDisplayName(contacts)).toSignerModel().copy(localKey = false)

fun JoinKey.getDisplayName(contacts: List<Contact>): String {
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