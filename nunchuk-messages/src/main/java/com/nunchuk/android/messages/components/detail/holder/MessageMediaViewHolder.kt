package com.nunchuk.android.messages.components.detail.holder

import android.content.res.Resources
import android.util.Size
import android.view.Gravity
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.nunchuk.android.core.di.singletonEntryPoint
import com.nunchuk.android.core.util.orDefault
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.detail.NunchukMediaMessage
import com.nunchuk.android.messages.databinding.ItemMessageMediaBinding
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import org.matrix.android.sdk.api.session.content.ContentUrlResolver
import org.matrix.android.sdk.api.util.MimeTypes
import org.matrix.android.sdk.api.util.MimeTypes.isMimeTypeVideo

class MessageMediaViewHolder(
    private val binding: ItemMessageMediaBinding,
    private val onOpenMediaViewer: (eventId: String) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    private val sessionHolder = binding.root.context.singletonEntryPoint().sessionHolder()

    init {
        binding.root.setOnDebounceClickListener {
            onOpenMediaViewer((it.tag as NunchukMediaMessage).eventId)
        }
    }

    fun bind(message: NunchukMediaMessage) {
        binding.root.tag = message
        val size = processSize(message.width.orDefault(0), message.height.orDefault(0))
        binding.containerMedia.updateLayoutParams<FrameLayout.LayoutParams> {
            gravity = if (message.isMine) Gravity.END else Gravity.START
        }
        binding.image.updateLayoutParams {
            width = size.width
            height = size.height
        }
        binding.videoThumb.isVisible =
            message.mimeType?.isMimeTypeVideo() == true || message.mimeType == MimeTypes.Gif
        load(message, size)
    }

    private fun load(message: NunchukMediaMessage, size: Size) {
        val transform =
            RoundedCorners(binding.root.context.resources.getDimensionPixelSize(R.dimen.nc_padding_24))
        if (message.elementToDecrypt != null) {
            // Encrypted image
            Glide.with(binding.image).load(message.content).transform(transform).into(binding.image)
        } else {
            // Clear image
            val contentUrlResolver =
                sessionHolder.getSafeActiveSession()?.contentUrlResolver() ?: return
            val resolvedUrl = contentUrlResolver.resolveThumbnail(
                message.content, size.width, size.height, ContentUrlResolver.ThumbnailMethod.SCALE
            )
            // Fallback to base url
                ?: message.content.takeIf { it.startsWith("content://") }

            Glide.with(binding.image).load(resolvedUrl).transform(transform).into(binding.image)
        }
    }

    private fun processSize(actualWidth: Int, actualHeight: Int): Size {
        if (actualWidth <= 0 || actualHeight <= 0) return Size(0, 0)
        return Size(
            MAX_IMAGE_WIDTH,
            MAX_IMAGE_WIDTH * actualHeight / actualWidth,
        )
    }

    companion object {
        private val MAX_IMAGE_WIDTH = Resources.getSystem().displayMetrics.widthPixels * 2 / 3
    }
}