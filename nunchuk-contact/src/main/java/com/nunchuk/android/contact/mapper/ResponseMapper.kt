package com.nunchuk.android.contact.mapper

import com.nunchuk.android.model.Contact
import com.nunchuk.android.model.ReceiveContact
import com.nunchuk.android.model.SentContact
import com.nunchuk.android.model.UserResponse

internal fun UserResponse.toModel() = Contact(
    id = id,
    name = name,
    email = email,
    gender = gender.orEmpty(),
    avatar = avatar.orEmpty(),
    status = status.orEmpty(),
    chatId = chatId,
    loginType = loginType,
    username = username
)

internal fun List<UserResponse>.toSentContacts() = map { SentContact(it.toModel()) }

internal fun List<UserResponse>.toReceiveContacts() = map { ReceiveContact(it.toModel()) }

