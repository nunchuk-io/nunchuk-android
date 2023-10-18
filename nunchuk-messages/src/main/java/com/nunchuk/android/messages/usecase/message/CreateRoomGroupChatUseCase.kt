package com.nunchuk.android.messages.usecase.message

import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.GROUP_CHAT_ROOM_TYPE
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.repository.SettingRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import org.matrix.android.sdk.api.session.room.model.RoomDirectoryVisibility
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomPreset
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomStateEvent
import javax.inject.Inject

class CreateRoomGroupChatUseCase @Inject constructor(
    private val sessionHolder: SessionHolder,
    private val settingRepository: SettingRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UseCase<CreateRoomGroupChatUseCase.Param, String>(ioDispatcher) {

    override suspend fun execute(parameters: Param): String {
        settingRepository.syncRoomSuccess.filter { it }.first()
        val session = sessionHolder.getSafeActiveSession()
            ?: throw NullPointerException("Can not get active session")
        val userIds = parameters.group.members.filter { it.isContact() && it.user?.chatId != null }
            .map { it.user?.chatId!! }
        val params = CreateRoomParams().apply {
            visibility = RoomDirectoryVisibility.PRIVATE
            isDirect = true
            this.invitedUserIds.addAll(userIds)
            preset = CreateRoomPreset.PRESET_PRIVATE_CHAT
            this.roomType = GROUP_CHAT_ROOM_TYPE
            creationContent = mutableMapOf(
                "nunchuk_config" to mapOf(
                    "group_id" to parameters.group.id
                )
            )
            this.initialStates = mutableListOf(CreateRoomStateEvent(
                type = GROUP_CHAT_ROOM_TYPE,
                content = mutableMapOf("group_id" to parameters.group.id),
                stateKey = ""
            ))
        }
        val roomId = session.roomService().createRoom(params)
        session.roomService().getRoom(roomId)?.also { room ->
            if (room.roomSummary()?.tags?.any { tag -> tag.name == GROUP_CHAT_ROOM_TYPE } == false) {
                room.tagsService().addTag(GROUP_CHAT_ROOM_TYPE, 1.0)
            }
        } ?: throw NullPointerException("Can not get room")
        delay(500L)
        return roomId
    }

    class Param(val group: ByzantineGroup)
}

