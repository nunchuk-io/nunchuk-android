package com.nunchuk.android.contact.mapper

import com.nunchuk.android.model.Contact
import com.nunchuk.android.model.UserResponse
import com.nunchuk.android.persistence.entity.ContactEntity

internal fun ContactEntity.toModel() = Contact(
    id = id,
    name = name,
    email = email,
    gender = gender,
    avatar = avatar,
    status = status,
    chatId = chatId,
    loginType = loginType.orEmpty(),
    username = username.orEmpty()
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
    accountId = accountId,
    loginType = loginType.orEmpty(),
    username = username.orEmpty()
)

internal fun List<UserResponse>.toEntities(accountId: String) = map { it.toEntity(accountId) }