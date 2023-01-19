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

class RoomMediaAdapter(private val items: List<RoomMediaSource>, private val lifecycleOwner: LifecycleOwner) :
    RecyclerView.Adapter<BaseMediaViewHolder>() {

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
}