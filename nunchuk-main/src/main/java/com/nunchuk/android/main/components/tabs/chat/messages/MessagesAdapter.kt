package com.nunchuk.android.main.components.tabs.chat.messages

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.base.BaseViewHolder
import com.nunchuk.android.main.R
import com.nunchuk.android.main.databinding.ItemMessageBinding
import com.nunchuk.android.messages.model.Message
import com.nunchuk.android.widget.util.inflate

internal class MessagesAdapter(
    private val listener: (Message) -> Unit
) : RecyclerView.Adapter<MessageViewHolder>() {

    internal var items: List<Message> = ArrayList()
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
    val listener: (Message) -> Unit
) : BaseViewHolder<Message>(itemView) {

    private val binding = ItemMessageBinding.bind(itemView)

    override fun bind(data: Message) {
        binding.name.text = data.contactName
        binding.message.text = data.lastMessage
        binding.count.text = "${data.messageCount}"
        binding.time.text = data.time
        binding.root.setOnClickListener { listener(data) }
    }

}