package com.nunchuk.android.messages.components.detail.holder

import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.messages.components.detail.Message
import com.nunchuk.android.messages.databinding.ItemMessagePartnerBinding

internal class MessagePartnerHolder(val binding: ItemMessagePartnerBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(messageData: Message) {
        val userName = messageData.sender
        binding.avatar.text = userName.shorten()
        binding.sender.text = userName
        binding.message.text = messageData.content
    }

}