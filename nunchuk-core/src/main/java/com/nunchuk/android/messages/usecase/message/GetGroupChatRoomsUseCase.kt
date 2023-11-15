package com.nunchuk.android.messages.usecase.message

import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.GROUP_CHAT_ROOM_TYPE
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.GroupChatRoom
import com.nunchuk.android.model.byzantine.isMasterOrAdmin
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import org.matrix.android.sdk.api.query.QueryStringValue
import javax.inject.Inject

class GetGroupChatRoomsUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    private val sessionHolder: SessionHolder,
    private val accountManager: AccountManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<GetGroupChatRoomsUseCase.Params, List<GroupChatRoom>>(ioDispatcher) {

    override suspend fun execute(parameters: Params): List<GroupChatRoom> {
        val session = sessionHolder.getSafeActiveSession()
            ?: throw NullPointerException("Can not get active session")
        val groupChatRooms = ArrayList<GroupChatRoom>()
        parameters.roomIds.forEach { roomId ->
            session.roomService().getRoom(roomId)?.let {
                val state =
                    it.stateService().getStateEvent(GROUP_CHAT_ROOM_TYPE, QueryStringValue.IsEmpty)
                val groupId = state?.content?.get("group_id") as? String
                groupId?.let {
                    val groupChatRoom = GroupChatRoom(
                        groupId = groupId,
                        roomId = roomId,
                        isMasterOrAdmin = isMasterOrAdmin(repository.getLocalGroup(groupId))
                    )
                    groupChatRooms.add(groupChatRoom)
                }
            }
        }
        return groupChatRooms
    }

    private fun isMasterOrAdmin(group: ByzantineGroup?): Boolean {
        if (group == null) return false
        return group.members.firstOrNull {
            it.emailOrUsername == accountManager.getAccount().email || it.emailOrUsername == accountManager.getAccount().username
        }?.role.toRole.isMasterOrAdmin
    }

    class Params(val roomIds: List<String>)
}