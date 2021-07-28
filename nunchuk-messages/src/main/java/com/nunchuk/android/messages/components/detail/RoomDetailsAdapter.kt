package com.nunchuk.android.messages.components.detail

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.messages.databinding.ItemMessageMeBinding
import com.nunchuk.android.messages.databinding.ItemMessagePartnerBinding

class RoomDetailsAdapter(
    val context: Context
) : Adapter<ViewHolder>() {

    internal var messages: List<Message> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = when (viewType) {
        MessageType.CHAT_MINE.index -> MineViewHolder(
            ItemMessageMeBinding.inflate(LayoutInflater.from(context), parent, false)
        )
        MessageType.CHAT_PARTNER.index -> PartnerHolder(
            ItemMessagePartnerBinding.inflate(LayoutInflater.from(context), parent, false)
        )
        else -> throw IllegalArgumentException("Invalid type")
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    override fun getItemViewType(position: Int): Int {
        return messages[position].type
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val messageData = messages[position]
        when (getItemViewType(position)) {
            MessageType.CHAT_MINE.index -> (holder as MineViewHolder).bind(messageData)
            MessageType.CHAT_PARTNER.index -> (holder as PartnerHolder).bind(messageData)
        }
    }

    internal class MineViewHolder(val binding: ItemMessageMeBinding) : ViewHolder(binding.root) {

        fun bind(messageData: Message) {
            binding.message.text = messageData.content
        }

    }

    internal class PartnerHolder(val binding: ItemMessagePartnerBinding) : ViewHolder(binding.root) {

        fun bind(messageData: Message) {
            val userName = messageData.sender
            binding.avatar.text = userName.shorten()
            binding.sender.text = userName
            binding.message.text = messageData.content
        }

    }

}