package com.nunchuk.android.main.components.tabs.chat.messages

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.base.BaseViewHolder
import com.nunchuk.android.main.R
import com.nunchuk.android.main.databinding.ItemMessageBinding
import com.nunchuk.android.widget.util.inflate
import org.matrix.android.sdk.api.session.room.model.RoomSummary

internal class MessagesAdapter(
    private val listener: (RoomSummary) -> Unit
) : RecyclerView.Adapter<MessageViewHolder>() {

    internal var items: List<RoomSummary> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MessageViewHolder(
        parent.inflate(R.layout.item_message),
        listener
    )

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

}

internal class MessageViewHolder(
    itemView: View,
    val listener: (RoomSummary) -> Unit
) : BaseViewHolder<RoomSummary>(itemView) {

    private val binding = ItemMessageBinding.bind(itemView)

    override fun bind(data: RoomSummary) {
        binding.name.text = data.displayName
        binding.message.text = "Last message: ..."
        binding.count.text = "${data.notificationCount}"
        binding.time.text = "1m"
        binding.root.setOnClickListener { listener(data) }
    }

}