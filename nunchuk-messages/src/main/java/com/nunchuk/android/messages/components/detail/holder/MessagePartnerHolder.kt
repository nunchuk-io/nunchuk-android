package com.nunchuk.android.messages.components.detail.holder

import android.text.method.LinkMovementMethod
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.loader.ImageLoader
import com.nunchuk.android.core.util.displayAvatar
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.messages.components.detail.MatrixMessage
import com.nunchuk.android.messages.databinding.ItemMessagePartnerBinding
import com.nunchuk.android.messages.util.displayNameOrId

internal class MessagePartnerHolder(
    private val imageLoader: ImageLoader,
    val binding: ItemMessagePartnerBinding,
    private val isGroupChat: Boolean,
    private val longPressListener: (message: MatrixMessage, position: Int) -> Unit,
    private val checkedChangeListener: (position: Int) -> Unit,
    private val onMessageRead: (eventId: String) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(messageData: MatrixMessage, position: Int) {
        binding.message.movementMethod = LinkMovementMethod.getInstance()
        val userName = messageData.sender.displayNameOrId()
        binding.avatar.displayAvatar(imageLoader, messageData.sender.avatarUrl, userName.shorten())
        binding.sender.text = userName
        binding.sender.isVisible = isGroupChat
        binding.message.text = messageData.content

        binding.cbSelect.setOnClickListener {
            checkedChangeListener.invoke(position)
        }
        binding.cbSelect.isChecked = messageData.selected && messageData.isSelectEnable
        if (messageData.isSelectEnable) {
            // don't allow long press when already in select mode
            binding.root.setOnLongClickListener(null)
            binding.message.setOnLongClickListener(null)
        } else {
            binding.root.setOnLongClickListener {
                binding.message.movementMethod = null
                longPressListener.invoke(messageData, position)
                true
            }
            binding.message.setOnLongClickListener {
                binding.message.movementMethod = null
                longPressListener.invoke(messageData, position)
                true
            }
        }
        binding.cbSelect.isVisible = messageData.isSelectEnable
        onMessageRead(messageData.timelineEvent.eventId)
    }

}