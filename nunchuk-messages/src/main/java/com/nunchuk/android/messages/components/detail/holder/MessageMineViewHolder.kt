package com.nunchuk.android.messages.components.detail.holder

import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.util.getString
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.detail.Message
import com.nunchuk.android.messages.databinding.ItemMessageMeBinding

internal class MessageMineViewHolder(val binding: ItemMessageMeBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(messageData: Message) {
        binding.message.text = messageData.content
        val state = messageData.state
        if (state.isSent()) {
            binding.status.text = getString(R.string.nc_message_status_delivered)
        } else if (state.isSending() || state.isInProgress()) {
            binding.status.text = getString(R.string.nc_message_status_sending)
        } else if (state.hasFailed()) {
            binding.status.text = getString(R.string.nc_message_status_failed)
        } else {
            binding.status.text = getString(R.string.nc_message_status_unknown)
        }
    }

}