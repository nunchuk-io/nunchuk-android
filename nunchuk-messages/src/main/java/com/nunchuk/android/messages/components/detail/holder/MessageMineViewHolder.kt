package com.nunchuk.android.messages.components.detail.holder

import android.text.method.LinkMovementMethod
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.util.getString
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.detail.MatrixMessage
import com.nunchuk.android.messages.databinding.ItemMessageMeBinding

internal class MessageMineViewHolder(
    val binding: ItemMessageMeBinding,
    private val longPressListener: (message: MatrixMessage, position: Int) -> Unit,
    private val checkedChangeListener: (position: Int) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(messageData: MatrixMessage, position: Int) {
        binding.message.movementMethod = LinkMovementMethod.getInstance()
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
    }

}