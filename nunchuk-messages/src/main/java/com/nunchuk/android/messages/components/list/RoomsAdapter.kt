package com.nunchuk.android.messages.components.list

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.base.BaseViewHolder
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.databinding.ItemRoomBinding
import com.nunchuk.android.messages.util.*
import com.nunchuk.android.widget.swipe.SwipeLayout
import com.nunchuk.android.widget.util.inflate
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import java.util.*

class RoomAdapter(
    private val currentName: String,
    private val enterRoom: (RoomSummary) -> Unit,
    private val removeRoom: (RoomSummary) -> Unit
) : RecyclerView.Adapter<RoomViewHolder>() {

    internal var roomWallets: List<String> = ArrayList()

    internal var roomSummaries: List<RoomSummary> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = RoomViewHolder(
        parent.inflate(R.layout.item_room),
        currentName,
        enterRoom,
        removeRoom
    )

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        holder.bind(roomWallets to roomSummaries[position])
    }

    override fun getItemCount() = roomSummaries.size

}

class RoomViewHolder(
    itemView: View,
    private val currentName: String,
    private val enterRoom: (RoomSummary) -> Unit,
    private val removeRoom: (RoomSummary) -> Unit
) : BaseViewHolder<Pair<List<String>, RoomSummary>>(itemView) {

    private val binding = ItemRoomBinding.bind(itemView)

    override fun bind(data: Pair<List<String>, RoomSummary>) {
        val roomWallets = data.first
        val roomSummary = data.second
        val roomName = roomSummary.getRoomName(currentName)
        binding.name.text = roomName
        roomSummary.latestPreviewableEvent?.let {
            binding.message.text = it.lastMessage()
            binding.time.text = it.root.originServerTs?.let { time -> Date(time).formatMessageDate() } ?: "-"
        }
        binding.message.isVisible = roomSummary.latestPreviewableEvent != null
        binding.time.isVisible = roomSummary.latestPreviewableEvent != null

        val isGroupChat = !roomSummary.isDirectChat()
        if (isGroupChat) {
            binding.avatarHolder.text = ""
            binding.badge.text = "${roomSummary.getMembersCount()}"
        } else {
            binding.avatarHolder.text = roomName.shorten()
        }
        binding.badge.isVisible = isGroupChat
        binding.avatar.isVisible = isGroupChat
        binding.count.isVisible = roomSummary.hasUnreadMessages && (roomSummary.notificationCount > 0)
        binding.count.text = "${roomSummary.notificationCount}"
        binding.shareIcon.isVisible = roomSummary.roomId in roomWallets

        binding.itemLayout.setOnClickListener { enterRoom(roomSummary) }
        binding.delete.setOnClickListener { removeRoom(roomSummary) }

        binding.swipeLayout.showMode = SwipeLayout.ShowMode.LayDown
        binding.swipeLayout.addDrag(SwipeLayout.DragEdge.Left, binding.actionLayout)
    }

}
