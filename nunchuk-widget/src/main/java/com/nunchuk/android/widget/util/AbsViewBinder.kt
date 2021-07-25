package com.nunchuk.android.widget.util

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding

abstract class AbsViewBinder<in Model, out Binding : ViewBinding> protected constructor(
    protected val container: ViewGroup,
    private val models: List<Model>
) {

    protected abstract fun initializeBinding(): Binding

    protected val context: Context = container.context

    protected val inflater: LayoutInflater = LayoutInflater.from(context)

    protected abstract fun bindItem(position: Int, model: Model)

    fun bindItems() {
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
    }

    private fun addChildViews(container: ViewGroup, size: Int) {
        for (i in 0 until size) {
            container.addView(initializeBinding().root)
        }
    }

}

