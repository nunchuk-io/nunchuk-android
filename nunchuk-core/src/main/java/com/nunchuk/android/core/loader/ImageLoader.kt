/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

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
    fun loadImage(
        url: String,
        imageView: ImageView,
        onSuccess: () -> Unit = {},
        onFailed: () -> Unit = {},
        roundedImage: Boolean = true
    )
}

@Singleton
class ImageLoaderImpl @Inject constructor(
    context: Context,
    private val sessionHolder: SessionHolder
) : ImageLoader {

    private val glideRequests: RequestManager = Glide.with(context)

    override fun loadImage(
        url: String,
        imageView: ImageView,
        onSuccess: () -> Unit,
        onFailed: () -> Unit,
        roundedImage: Boolean
    ) {
        val resolvedUrl = resolvedUrl(url)
        glideRequests.load(resolvedUrl).apply {
            if (roundedImage) {
                circleCrop()
            }
            onSuccess()
        }.listener(object : RequestListener<Drawable> {

            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>,
                isFirstResource: Boolean,
            ): Boolean {
                onFailed()
                e?.let(CrashlyticsReporter::recordException)
                return false
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable>?,
                dataSource: DataSource,
                isFirstResource: Boolean,
            ): Boolean = false

        }).into(imageView)
    }

    private fun resolvedUrl(avatarUrl: String?) = sessionHolder.getSafeActiveSession()?.contentUrlResolver()?.resolveThumbnail(
        avatarUrl,
        THUMBNAIL_SIZE,
        THUMBNAIL_SIZE,
        ContentUrlResolver.ThumbnailMethod.SCALE
    )

    companion object {
        private const val THUMBNAIL_SIZE = 100
    }
}