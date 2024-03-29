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

import com.nunchuk.android.core.domain.settings.GetChainSettingFlowUseCase
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.SUPPORT_ROOM_TYPE
import com.nunchuk.android.core.util.SUPPORT_ROOM_USER_ID
import com.nunchuk.android.core.util.SUPPORT_TEST_NET_ROOM_TYPE
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.messages.components.list.isSupportRoom
import com.nunchuk.android.repository.SettingRepository
import com.nunchuk.android.type.Chain
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomDirectoryVisibility
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomPreset
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import javax.inject.Inject

class GetOrCreateSupportRoomUseCase @Inject constructor(
    private val sessionHolder: SessionHolder,
    private val getChainSettingFlowUseCase: GetChainSettingFlowUseCase,
    private val settingRepository: SettingRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UseCase<Unit, Room>(ioDispatcher) {

    override suspend fun execute(parameters: Unit): Room {
        settingRepository.syncRoomSuccess.filter { it }.first()
        val session = sessionHolder.getSafeActiveSession()
            ?: throw NullPointerException("Can not get active session")
        val roomType = getRoomType()
        val roomId = session.roomService().getRoomSummaries(roomSummaryQueryParams {
            memberships = Membership.activeMemberships()
        }).find { roomSummary ->
            roomSummary.isSupportRoom()
        }?.roomId ?: run {
            val params = CreateRoomParams().apply {
                visibility = RoomDirectoryVisibility.PRIVATE
                isDirect = true
                this.invitedUserIds.addAll(listOf(SUPPORT_ROOM_USER_ID))
                preset = CreateRoomPreset.PRESET_TRUSTED_PRIVATE_CHAT
                this.roomType = roomType
            }
            session.roomService().createRoom(params)
        }
        val room = session.roomService().getRoom(roomId)?.also { room ->
            if (room.roomSummary()?.tags?.any { tag -> tag.name == roomType } == false) {
                room.tagsService().addTag(roomType, 1.0)
            }
        } ?: throw NullPointerException("Can not get room")
        delay(500L)
        return room
    }

    private suspend fun getRoomType(): String {
        val chain = getChainSettingFlowUseCase(Unit).firstOrNull()?.getOrNull()
        return if (chain == Chain.MAIN) {
            SUPPORT_ROOM_TYPE
        } else {
            SUPPORT_TEST_NET_ROOM_TYPE
        }
    }
}

