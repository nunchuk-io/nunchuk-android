package com.nunchuk.android.core.loader

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.utils.CrashlyticsReporter
import org.matrix.android.sdk.api.session.content.ContentUrlResolver
import javax.inject.Inject
import javax.inject.Singleton

interface ImageLoader {
    fun loadImage(url: String, imageView: ImageView, onFailed: () -> Unit = {}, roundedImage: Boolean = true)
}

@Singleton
class ImageLoaderImpl @Inject constructor(context: Context) : ImageLoader {

    private val glideRequests: RequestManager = Glide.with(context)

    override fun loadImage(url: String, imageView: ImageView, onFailed: () -> Unit, roundedImage: Boolean) {
        val resolvedUrl = resolvedUrl(url)
        glideRequests.load(resolvedUrl).apply {
            if (roundedImage) {
                circleCrop()
            }
        }.listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                onFailed()
                e?.let(CrashlyticsReporter::recordException)
                return false
            }

            override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                return false
            }

        }).into(imageView)
    }

    private fun resolvedUrl(avatarUrl: String?) = SessionHolder.activeSession?.contentUrlResolver()?.resolveThumbnail(
        avatarUrl,
        THUMBNAIL_SIZE,
        THUMBNAIL_SIZE,
        ContentUrlResolver.ThumbnailMethod.SCALE
    )

    companion object {
        private const val THUMBNAIL_SIZE = 100
    }
}