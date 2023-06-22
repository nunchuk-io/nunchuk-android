/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
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

package com.nunchuk.android.messages.usecase.message

import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.messages.model.RoomNotFoundException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface AddTagRoomUseCase {
    fun execute(tagName: String, roomId: String): Flow<Unit>
}

internal class AddTagRoomUseCaseImpl @Inject constructor(
    sessionHolder: SessionHolder
) : BaseMessageUseCase(sessionHolder), AddTagRoomUseCase {

    override fun execute(tagName: String, roomId: String) = flow {
        emit(
            session.roomService().getRoom(roomId)?.tagsService()?.addTag(tagName, 1.0) ?: throw RoomNotFoundException(roomId)
        )
    }
}