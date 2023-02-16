/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.messages.glide

import android.content.Context
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import com.nunchuk.android.core.di.singletonEntryPoint
import com.nunchuk.android.core.matrix.coroutineScope
import com.nunchuk.android.messages.components.detail.NunchukMedia
import com.nunchuk.android.messages.util.LocalFilesHelper
import com.nunchuk.android.messages.util.downloadFile
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import java.io.InputStream

class VectorGlideModelLoaderFactory(private val context: Context) :
    ModelLoaderFactory<NunchukMedia, InputStream> {

    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<NunchukMedia, InputStream> {
        return VectorGlideModelLoader(context)
    }

    override fun teardown() {
        // Is there something to do here?
    }
}

class VectorGlideModelLoader(private val context: Context) :
    ModelLoader<NunchukMedia, InputStream> {
    override fun handles(model: NunchukMedia): Boolean {
        // Always handle
        return true
    }

    override fun buildLoadData(
        model: NunchukMedia,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<InputStream> {
        return ModelLoader.LoadData(
            ObjectKey(model.eventId),
            VectorGlideDataFetcher(context, model)
        )
    }
}

class VectorGlideDataFetcher(
    private val context: Context,
    private val data: NunchukMedia,
) : DataFetcher<InputStream> {

    private val activeSessionHolder = context.singletonEntryPoint().sessionHolder()

    override fun getDataClass(): Class<InputStream> {
        return InputStream::class.java
    }

    private var job: Job? = null

    override fun cleanup() {
        cancel()
    }

    override fun getDataSource(): DataSource = DataSource.REMOTE

    override fun cancel() {
        job?.cancel()
    }

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        Timber.v("Load data: $data")
        if (LocalFilesHelper.isLocalFile(context, data.url)) {
            LocalFilesHelper.openInputStream(context, data.url)?.use {
                callback.onDataReady(it)
            }
            return
        }

        val fileService =
            activeSessionHolder.getSafeActiveSession()?.fileService() ?: return Unit.also {
                callback.onLoadFailed(IllegalArgumentException("No File service"))
            }
        // Use the file vector service, will avoid flickering and redownload after upload
        job = activeSessionHolder.getSafeActiveSession()?.coroutineScope?.launch {
            val result = runCatching {
                fileService.downloadFile(data)
            }
            result.fold(
                { callback.onDataReady(it.inputStream()) },
                { callback.onLoadFailed(it as? Exception ?: IOException(it.localizedMessage)) }
            )
        }
    }
}
