package com.nunchuk.android.messages.components.detail.viewer

import com.nunchuk.android.messages.components.detail.model.RoomMediaSource
import com.nunchuk.android.messages.databinding.ItemImageViewerBinding
import org.matrix.android.sdk.api.session.room.model.message.getFileUrl

class ZoomableImageViewHolder(
    private val binding: ItemImageViewerBinding
) : BaseMediaViewHolder(binding) {
    override fun bind(item: RoomMediaSource) {
        val data = item as RoomMediaSource.Image

        buildRequestManager(data.content.getFileUrl(), data.content.encryptedFileInfo, data.allowNonMxcUrls, binding.image)
            .override(maxImageWidth, maxImageHeight).fitCenter().into(binding.image)
    }
}