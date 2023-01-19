package com.nunchuk.android.messages.components.detail.viewer

import com.nunchuk.android.messages.components.detail.model.RoomMediaSource
import com.nunchuk.android.messages.databinding.ItemAnimatedImageViewerBinding
import org.matrix.android.sdk.api.session.room.model.message.getFileUrl

class AnimatedImageViewHolder(
    private val binding: ItemAnimatedImageViewerBinding
) : BaseMediaViewHolder(binding) {

    override fun bind(item: RoomMediaSource) {
        val data = item as RoomMediaSource.AnimatedImage

        buildRequestManager(data.content.getFileUrl(), data.content.encryptedFileInfo, data.allowNonMxcUrls, binding.image)
            .override(maxImageWidth, maxImageHeight)
            .fitCenter()
            .into(binding.image)
    }
}