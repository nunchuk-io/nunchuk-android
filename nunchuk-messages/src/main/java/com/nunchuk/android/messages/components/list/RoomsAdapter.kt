package com.nunchuk.android.messages.components.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.nunchuk.android.core.base.ItemComparator
import com.nunchuk.android.core.base.RecyclerAdapter
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.messages.databinding.ItemRoomBinding
import com.nunchuk.android.messages.util.*
import com.nunchuk.android.widget.swipe.SwipeLayout
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import java.util.*

class RoomAdapter(
    private val currentName: String,
    private val enterRoom: (RoomSummary) -> Unit,
    private val removeRoom: (RoomSummary) -> Unit
) : RecyclerAdapter<RoomViewHolder, RoomSummary>(
    comparator = RoomSummaryComparator
) {

    var roomWallets: List<String> = ArrayList()

    override fun getViewHolder(parent: ViewGroup, viewType: Int) = RoomViewHolder(
        ItemRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        roomWallets,
        currentName,
        enterRoom,
        removeRoom
    )
}

class RoomViewHolder(
    binding: ItemRoomBinding,
    private val roomWallets: List<String>,
    private val currentName: String,
    private val enterRoom: (RoomSummary) -> Unit,
    private val removeRoom: (RoomSummary) -> Unit
) : RecyclerAdapter.BaseViewHolder<ItemRoomBinding, RoomSummary>(binding) {

    override fun bind(data: RoomSummary) {
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
        binding.count.isVisible = data.hasUnreadMessages && (data.notificationCount > 0)
        binding.count.text = "${data.notificationCount}"
        binding.shareIcon.isVisible = data.roomId in roomWallets

        binding.itemLayout.setOnClickListener { enterRoom(data) }
        binding.delete.setOnClickListener { removeRoom(data) }

        binding.swipeLayout.showMode = SwipeLayout.ShowMode.LayDown
        binding.swipeLayout.addDrag(SwipeLayout.DragEdge.Left, binding.actionLayout)
    }

}

object RoomSummaryComparator : ItemComparator<RoomSummary> {

    override fun areItemsTheSame(
        item1: RoomSummary,
        item2: RoomSummary
    ) = (item1.roomId == item2.roomId)

    override fun areContentsTheSame(item1: RoomSummary, item2: RoomSummary): Boolean {
        return item1.roomId == item2.roomId
                && item1.roomType == item2.roomType
                && item1.hasUnreadMessages == item2.hasUnreadMessages
                && item1.hasNewMessages == item2.hasNewMessages
                && item1.notificationCount == item2.notificationCount
                && item1.latestPreviewableEvent?.root?.originServerTs == item2.latestPreviewableEvent?.root?.originServerTs
    }
}