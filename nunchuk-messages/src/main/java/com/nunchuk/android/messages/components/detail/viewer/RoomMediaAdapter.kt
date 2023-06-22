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

package com.nunchuk.android.messages.components.detail.viewer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.detail.model.RoomMediaSource
import com.nunchuk.android.messages.databinding.ItemAnimatedImageViewerBinding
import com.nunchuk.android.messages.databinding.ItemImageViewerBinding
import com.nunchuk.android.messages.databinding.ItemVideoViewerBinding

class RoomMediaAdapter(private val lifecycleOwner: LifecycleOwner) :
    RecyclerView.Adapter<BaseMediaViewHolder>() {
    val items: MutableList<RoomMediaSource> = mutableListOf()
    var recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseMediaViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.item_image_viewer -> ZoomableImageViewHolder(
                ItemImageViewerBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            )
            R.layout.item_animated_image_viewer -> AnimatedImageViewHolder(
                ItemAnimatedImageViewerBinding.inflate(inflater, parent, false)
            )
            R.layout.item_video_viewer -> VideoViewHolder(
                ItemVideoViewerBinding.inflate(
                    inflater,
                    parent,
                    false
                ),
                lifecycleOwner
            )
            else -> throw IllegalArgumentException("Does not support view type")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is RoomMediaSource.Image -> R.layout.item_image_viewer
            is RoomMediaSource.AnimatedImage -> R.layout.item_animated_image_viewer
            is RoomMediaSource.Video -> R.layout.item_video_viewer
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: BaseMediaViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun onViewAttachedToWindow(holder: BaseMediaViewHolder) {
        holder.onAttached()
    }

    override fun onViewRecycled(holder: BaseMediaViewHolder) {
        holder.onRecycled()
    }

    override fun onViewDetachedFromWindow(holder: BaseMediaViewHolder) {
        holder.onDetached()
    }

    fun getItem(position: Int) = items.getOrNull(position)

    fun isScaled(position: Int): Boolean {
        val holder = recyclerView?.findViewHolderForAdapterPosition(position)
        if (holder is ZoomableImageViewHolder) {
            return holder.binding.image.attacher.scale > 1f
        }
        return false
    }
}