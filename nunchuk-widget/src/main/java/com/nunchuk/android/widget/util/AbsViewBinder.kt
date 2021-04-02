package com.nunchuk.android.widget.util

import android.view.View
import android.view.ViewGroup

abstract class AbsViewBinder<in Model> protected constructor(
    protected val container: ViewGroup,
    private val models: List<Model>
) {

    protected abstract fun bindItem(model: Model): View

    fun bindItems() {
        if (invalidate()) return
        val size = models.size
        val childCount = container.childCount
        if (childCount == 0 || childCount < size) {
            addChildViews(container, size - childCount)
        } else if (childCount > size) {
            container.removeViews(0, childCount - size)
        }
        container.tag = models
    }

    protected fun invalidate(): Boolean {
        return models == container.tag
    }

    private fun addChildViews(container: ViewGroup, size: Int) {
        for (i in 0 until size) {
            container.addView(bindItem(models[i]))
        }
    }

}
