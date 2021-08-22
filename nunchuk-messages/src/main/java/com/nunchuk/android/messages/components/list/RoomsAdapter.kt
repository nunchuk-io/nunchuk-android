package com.nunchuk.android.messages.components.list

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.base.BaseViewHolder
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.databinding.ItemMessageBinding
import com.nunchuk.android.messages.util.*
import com.nunchuk.android.widget.swipe.SwipeLayout
import com.nunchuk.android.widget.util.inflate
import org.matrix.android.sdk.api.session.room.model.RoomSummary

class MessagesAdapter(
    private val currentName: String,
    private val dateFormatter: DateFormatter,
    private val enterRoom: (RoomSummary) -> Unit,
    private val removeRoom: (RoomSummary) -> Unit
) : RecyclerView.Adapter<MessageViewHolder>() {

    internal var items: List<RoomSummary> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MessageViewHolder(
        parent.inflate(R.layout.item_message),
        currentName,
        dateFormatter,
        enterRoom, removeRoom
    )

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

}

class MessageViewHolder(
    itemView: View,
    private val currentName: String,
    private val dateFormatter: DateFormatter,
    private val enterRoom: (RoomSummary) -> Unit,
    private val removeRoom: (RoomSummary) -> Unit
) : BaseViewHolder<RoomSummary>(itemView) {

    private val binding = ItemMessageBinding.bind(itemView)

    override fun bind(data: RoomSummary) {
        val roomName = data.getRoomName(currentName)
        binding.name.text = roomName
        data.latestPreviewableEvent?.let {
            binding.message.text = it.lastMessage()
            binding.time.text = it.root.originServerTs?.let(dateFormatter::formatDateAndTime) ?: "-"
        }
        val isGroupChat = data.isGroupChat()
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
        binding.itemLayout.setOnClickListener { enterRoom(data) }
        binding.delete.setOnClickListener { removeRoom(data) }

        binding.swipeLayout.showMode = SwipeLayout.ShowMode.PullOut
        binding.swipeLayout.addDrag(SwipeLayout.DragEdge.Left, binding.actionLayout)
    }

}
