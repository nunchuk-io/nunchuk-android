package com.nunchuk.android.messages.components.detail.viewer

import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.nunchuk.android.core.di.singletonEntryPoint
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.detail.NunchukMedia
import com.nunchuk.android.messages.components.detail.model.RoomMediaSource
import com.nunchuk.android.messages.glide.GlideApp
import com.nunchuk.android.messages.util.LocalFilesHelper

abstract class BaseMediaViewHolder(binding: ViewBinding) : ViewHolder(binding.root) {
    protected val sessionHolder = binding.root.context.singletonEntryPoint().sessionHolder()

    abstract fun bind(item: RoomMediaSource)
    open fun onAttached() {}
    open fun onDetached() {}
    open fun onRecycled() {}

    protected fun buildRequestManager(
        data: NunchukMedia,
        allowNonMxcUrls: Boolean,
        image: ImageView,
    ): RequestBuilder<Drawable> {
        val circularProgressDrawable = CircularProgressDrawable(itemView.context)
        circularProgressDrawable.strokeWidth = itemView.context.resources.getDimension(R.dimen.nc_padding_4)
        circularProgressDrawable.centerRadius = itemView.context.resources.getDimension(R.dimen.nc_padding_24)
        circularProgressDrawable.setColorSchemeColors(Color.WHITE)
        circularProgressDrawable.start()

        return if (data.elementToDecrypt != null) {
            // Encrypted image
            GlideApp.with(image).load(data).placeholder(circularProgressDrawable)
        } else {
            // Clear image
            val resolvedUrl = resolveUrl(data.url, allowNonMxcUrls)
            Glide.with(image).load(resolvedUrl).placeholder(circularProgressDrawable)
        }
    }

    private fun resolveUrl(url: String?, allowNonMxcUrls: Boolean) =
        (sessionHolder.getSafeActiveSession()?.contentUrlResolver()?.resolveFullSize(url)
            ?: url?.takeIf {
                LocalFilesHelper.isLocalFile(
                    itemView.context,
                    url
                ) && allowNonMxcUrls
            })

    protected val maxImageWidth = Resources.getSystem().displayMetrics.widthPixels
    protected val maxImageHeight = Resources.getSystem().displayMetrics.heightPixels
}