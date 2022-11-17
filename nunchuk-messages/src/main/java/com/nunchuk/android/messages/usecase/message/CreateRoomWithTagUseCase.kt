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

package com.nunchuk.android.messages.usecase.message

import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.messages.model.RoomWithTagCreationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.RoomDirectoryVisibility
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomPreset
import javax.inject.Inject

interface CreateRoomWithTagUseCase {
    fun execute(displayName: String, invitedUserIds: List<String>, tag: String): Flow<Room>
}

internal class CreateRoomWithTagUseCaseImpl @Inject constructor(
    sessionHolder: SessionHolder
) : BaseMessageUseCase(sessionHolder), CreateRoomWithTagUseCase {

    override fun execute(displayName: String, invitedUserIds: List<String>, tag: String) = flow {
        val params = CreateRoomParams().apply {
            visibility = RoomDirectoryVisibility.PRIVATE
            isDirect = invitedUserIds.size == 1
            this.invitedUserIds.addAll(invitedUserIds)
            preset = CreateRoomPreset.PRESET_PRIVATE_CHAT
            name = displayName
        }
        val room = session.roomService().getRoom(session.roomService().createRoom(params))
        room?.tagsService()?.addTag(tag, 1.0)
        emit(
            room ?: throw RoomWithTagCreationException()
        )
    }.flowOn(Dispatchers.IO)

}