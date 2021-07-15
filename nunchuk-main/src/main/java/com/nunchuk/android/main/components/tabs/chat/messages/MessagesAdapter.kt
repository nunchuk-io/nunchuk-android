package com.nunchuk.android.main.components.tabs.chat.messages

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.base.BaseViewHolder
import com.nunchuk.android.main.R
import com.nunchuk.android.main.databinding.ItemMessageBinding
import com.nunchuk.android.widget.swipe.SwipeLayout
import com.nunchuk.android.widget.util.inflate
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.getLastMessageContent
import org.matrix.android.sdk.api.session.room.timeline.getTextEditableContent
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

internal class MessagesAdapter(
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
        enterRoom, removeRoom
    )

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

}

internal class MessageViewHolder(
    itemView: View,
    private val enterRoom: (RoomSummary) -> Unit,
    private val removeRoom: (RoomSummary) -> Unit
) : BaseViewHolder<RoomSummary>(itemView) {

    private val binding = ItemMessageBinding.bind(itemView)

    override fun bind(data: RoomSummary) {
        binding.name.text = data.displayName
        data.latestPreviewableEvent?.let {
            binding.message.text = it.lastMessage()
            binding.time.text = format(it.root.originServerTs)
        }
        binding.count.isVisible = data.hasUnreadMessages && (data.notificationCount > 0)
        binding.count.text = "${data.notificationCount}"
        binding.itemLayout.setOnClickListener { enterRoom(data) }
        binding.delete.setOnClickListener { removeRoom(data) }

        binding.swipeLayout.showMode = SwipeLayout.ShowMode.PullOut
        binding.swipeLayout.addDrag(SwipeLayout.DragEdge.Left, binding.actionLayout)
    }

    private fun format(ts: Long?) = ts?.formatDate() ?: "-"

    private fun TimelineEvent.lastMessage(): CharSequence {
        val senderName = senderInfo.disambiguatedDisplayName
        val lastMessage = getTextEditableContent() ?: getLastMessageContent()?.body
        return "$senderName: $lastMessage"
    }

    private fun Long.formatDate(): String = SimpleDateFormat("dd/MM HH:mm", Locale.US).format(Date(this))

}