package com.nunchuk.android.messages.components.detail.holder

import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.loader.ImageLoader
import com.nunchuk.android.core.util.displayAvatar
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.messages.components.detail.Message
import com.nunchuk.android.messages.databinding.ItemMessagePartnerBinding
import com.nunchuk.android.messages.util.displayNameOrId

internal class MessagePartnerHolder(
    private val imageLoader: ImageLoader,
    val binding: ItemMessagePartnerBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(messageData: Message) {
        val userName = messageData.sender.displayNameOrId()
        binding.avatar.displayAvatar(imageLoader, messageData.sender.avatarUrl, userName.shorten())
        binding.sender.text = userName
        binding.message.text = messageData.content
    }

}