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

package com.nunchuk.android.core.repository

import com.nunchuk.android.persistence.dao.HandledEventDao
import com.nunchuk.android.persistence.entity.HandledEventEntity
import com.nunchuk.android.repository.HandledEventRepository
import javax.inject.Inject

class HandledEventRepositoryImpl @Inject constructor(
    private val handledEventDao: HandledEventDao
) : HandledEventRepository {
    override suspend fun isHandled(eventId: String): Boolean {
        return handledEventDao.getEvent(eventId) != null
    }

    override suspend fun save(eventId: String) {
        handledEventDao.insert(HandledEventEntity(eventId))
    }
}