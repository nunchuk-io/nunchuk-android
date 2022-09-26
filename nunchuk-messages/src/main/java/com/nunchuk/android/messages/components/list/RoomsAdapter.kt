package com.nunchuk.android.messages.components.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.messages.databinding.ItemRoomBinding
import com.nunchuk.android.messages.util.*
import com.nunchuk.android.widget.swipe.SwipeLayout
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import java.util.*

class RoomAdapter(
    private val currentName: String,
    private val enterRoom: (RoomSummary) -> Unit,
    private val removeRoom: (RoomSummary, hasSharedWallet: Boolean) -> Unit
) : ListAdapter<RoomSummary, RoomViewHolder>(
    RoomSummaryComparator
) {

    val roomWallets: MutableSet<String> = mutableSetOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        return RoomViewHolder(
            ItemRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            roomWallets,
            currentName,
            enterRoom,
            removeRoom
        )
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class RoomViewHolder(
    private val binding: ItemRoomBinding,
    private val roomWallets: MutableSet<String>,
    private val currentName: String,
    private val enterRoom: (RoomSummary) -> Unit,
    private val removeRoom: (RoomSummary, hasSharedWallet: Boolean) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(data: RoomSummary) {
        val roomName = data.getRoomName(currentName)
        binding.name.text = roomName
        data.latestPreviewableEvent?.let {
            binding.message.text = it.lastMessage()
            binding.time.text = it.root.originServerTs?.let { time -> Date(time).formatMessageDate() } ?: "-"
        }
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