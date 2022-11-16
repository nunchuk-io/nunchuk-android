/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

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