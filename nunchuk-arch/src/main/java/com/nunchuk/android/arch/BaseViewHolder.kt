package com.nunchuk.android.arch

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer

abstract class BaseViewHolder<in T> protected constructor(
    itemView: View
) : RecyclerView.ViewHolder(itemView), LayoutContainer {

    protected val context: Context by lazy { itemView.context }

    override val containerView: View
        get() = itemView

    abstract fun bind(data: T)
}