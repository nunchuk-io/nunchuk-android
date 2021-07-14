package com.nunchuk.android.messages.mapper

import com.nunchuk.android.messages.api.UserResponse
import com.nunchuk.android.messages.model.Contact
import com.nunchuk.android.persistence.entity.ContactEntity

internal fun ContactEntity.toModel() = Contact(
    id = id,
    name = name,
    email = email,
    gender = gender,
    avatar = avatar,
    status = status,
    chatId = chatId
)

internal fun List<ContactEntity>.toModels() = map(ContactEntity::toModel)

internal fun UserResponse.toEntity(accountId: String) = ContactEntity(
    id = id,
    name = name,
    email = email,
    gender = gender.orEmpty(),
    avatar = avatar.orEmpty(),
    status = status.orEmpty(),
    chatId = chatId,
    accountId = accountId
)

internal fun List<UserResponse>.toEntities(accountId: String) = map { it.toEntity(accountId) }