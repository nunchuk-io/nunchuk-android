package com.nunchuk.android.messages.mapper

import com.nunchuk.android.messages.api.UserResponse
import com.nunchuk.android.messages.model.Contact
import com.nunchuk.android.messages.model.ReceiveContact
import com.nunchuk.android.messages.model.SentContact

internal fun UserResponse.toModel() = Contact(
    id = id,
    name = name,
    email = email,
    gender = gender.orEmpty(),
    avatar = avatar.orEmpty(),
    status = status.orEmpty(),
    chatId = chatId
)

internal fun List<UserResponse>.toSentContacts() = map { SentContact(it.toModel()) }

internal fun List<UserResponse>.toReceiveContacts() = map { ReceiveContact(it.toModel()) }

