package com.nunchuk.android.messages.components.detail.holder

import android.view.Gravity
import android.widget.FrameLayout
import androidx.core.view.isGone
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.nunchuk.android.messages.components.detail.NunchukFileMessage
import com.nunchuk.android.messages.databinding.ItemFileAttachmentBinding

class MessageFileHolder(
    private val binding: ItemFileAttachmentBinding,
    private val onDownloadOrOpen: (media: NunchukFileMessage) -> Unit
) : ViewHolder(binding.root) {
    init {
        binding.root.setOnClickListener {
            onDownloadOrOpen(it.tag as NunchukFileMessage)
        }
    }
    fun bind(message: NunchukFileMessage) {
        binding.root.tag = message
        binding.containerAttachment.updateLayoutParams<FrameLayout.LayoutParams> {
            gravity = if (message.isMine) Gravity.END else Gravity.START
        }
        binding.tvFileName.text = message.filename
        binding.tvError.isGone = message.error.isNullOrEmpty()
        binding.tvError.text = message.error
    }
}