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

package com.nunchuk.android.messages.components.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.messages.databinding.ItemRoomBinding
import com.nunchuk.android.messages.util.lastMessage
import com.nunchuk.android.messages.util.time
import com.nunchuk.android.model.GroupChatRoom
import com.nunchuk.android.utils.formatMessageDate
import com.nunchuk.android.widget.swipe.SwipeLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import java.util.Date

class RoomAdapter(
    private val scope: CoroutineScope,
    private val sessionHolder: SessionHolder,
    private val currentName: String,
    private val enterRoom: (RoomSummary) -> Unit,
    private val removeRoom: (RoomSummary, hasSharedWallet: Boolean) -> Unit
) : ListAdapter<RoomSummary, RoomViewHolder>(RoomSummaryComparator) {

    val roomWallets: MutableSet<String> = mutableSetOf()
    val groupChatRooms: HashMap<String, GroupChatRoom> = hashMapOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        return RoomViewHolder(
            scope = scope,
            sessionHolder = sessionHolder,
            binding = ItemRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            roomWallets = roomWallets,
            groupChatRooms = groupChatRooms,
            currentName = currentName,
            enterRoom = enterRoom,
            removeRoom = removeRoom
        )
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewDetachedFromWindow(holder: RoomViewHolder) {
        holder.cancelHideMessageJob()
    }
}

class RoomViewHolder(
    private val scope: CoroutineScope,
    private val sessionHolder: SessionHolder,
    private val binding: ItemRoomBinding,
    private val roomWallets: MutableSet<String>,
    private val groupChatRooms: HashMap<String, GroupChatRoom>,
    private val currentName: String,
    private val enterRoom: (RoomSummary) -> Unit,
    private val removeRoom: (RoomSummary, hasSharedWallet: Boolean) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    private var hideMessageJob : Job? = null

    fun bind(data: RoomSummary) {
        val roomName = data.getRoomName(currentName)
        binding.name.text = roomName
        bindLastMessage(data)
        binding.message.isVisible = data.latestPreviewableEvent != null
        binding.time.isVisible = data.latestPreviewableEvent != null

        val isGroupChat = !data.isDirectChat()
        if (isGroupChat) {
            binding.avatarHolder.text = ""
            binding.badge.text = "${data.getMembersCount()}"
        } else {
            binding.avatarHolder.text = roomName.shorten()
        }
        binding.badge.isVisible = isGroupChat
        binding.avatar.isVisible = isGroupChat
        bindCount(data)
        binding.shareIcon.isVisible = data.roomId in roomWallets
        binding.encryptedIcon.isVisible = data.isEncrypted

        binding.itemLayout.setOnClickListener { enterRoom(data) }
        binding.delete.setOnClickListener { removeRoom(data, binding.shareIcon.isVisible) }

        binding.swipeLayout.showMode = SwipeLayout.ShowMode.LayDown
        binding.swipeLayout.addDrag(SwipeLayout.DragEdge.Left, binding.actionLayout)
        binding.swipeLayout.isSwipeEnabled =
            groupChatRooms[data.roomId]?.isMasterOrAdmin == true || groupChatRooms[data.roomId] == null
    }

    private fun bindLastMessage(summary: RoomSummary) {
        val room =
            sessionHolder.getSafeActiveSession()?.roomService()?.getRoom(summary.roomId) ?: return
        val stateEvent =
            room.stateService().getStateEvent("m.room.retention", QueryStringValue.IsEmpty)
        val maxLifetime =
            stateEvent?.content?.get("max_lifetime")?.toString()?.toDoubleOrNull()?.toLong()
        summary.latestPreviewableEvent?.let { event ->
            val age = System.currentTimeMillis() - event.time()
            binding.message.text = event.lastMessage(itemView.context).takeIf {
                age <= (maxLifetime ?: Long.MAX_VALUE)
            }
            binding.time.text =
                event.root.originServerTs?.let { time -> Date(time).formatMessageDate() } ?: "-"

            if (maxLifetime != null && age < maxLifetime) {
                hideMessageJob = scope.launch {
                    delay(maxLifetime - age)
                    binding.message.text = ""
                }
            }
        }
    }

    fun cancelHideMessageJob() {
        hideMessageJob?.cancel()
    }

    private fun bindCount(data: RoomSummary) {
        val notificationCount = data.notificationCount
        binding.count.isVisible = data.hasUnreadMessages && (notificationCount > 0)
        if (notificationCount <= 99) {
            binding.count.text = "$notificationCount"
        } else {
            binding.count.text = "99+"
        }
    }
}

object RoomSummaryComparator : DiffUtil.ItemCallback<RoomSummary>() {

    override fun areItemsTheSame(
        item1: RoomSummary,
        item2: RoomSummary
    ) = (item1.roomId == item2.roomId)

    override fun areContentsTheSame(item1: RoomSummary, item2: RoomSummary): Boolean {
        return item1 == item2
    }
}