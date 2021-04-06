package com.nunchuk.android.widget.util

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes

abstract class AbsViewBinder<in Model> protected constructor(
    protected val container: ViewGroup,
    private val models: List<Model>
) {

    protected val context: Context = container.context

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    @get:LayoutRes
    protected abstract val layoutId: Int

    protected abstract fun bindItem(position: Int, model: Model)

    fun bindItems() {
        if (invalidate()) return
        val size = models.size
        val childCount = container.childCount
        if (childCount == 0 || childCount < size) {
            addChildViews(container, size - childCount)
        } else if (childCount > size) {
            container.removeViews(0, childCount - size)
        }
        for (i in 0 until size) {
            bindItem(i, models[i])
        }
        container.tag = models
    }

    private fun invalidate(): Boolean {
        return models == container.tag
    }

    private fun addChildViews(container: ViewGroup, size: Int) {
        for (i in 0 until size) {
            container.addView(inflater.inflate(layoutId, container, false))
        }
    }

}

