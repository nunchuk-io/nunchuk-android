package com.nunchuk.android.core.base

import androidx.recyclerview.widget.DiffUtil

class DiffUtilCallback<in T>(
    private val oldItems: List<T>,
    private val newItems: List<T>,
    private val comparator: ItemComparator<T>
) : DiffUtil.Callback() {

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        return oldItem != null && newItem != null && comparator.areItemsTheSame(oldItem, newItem)
    }

    override fun getOldListSize() = oldItems.size

    override fun getNewListSize() = newItems.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        return oldItem != null && newItem != null && comparator.areContentsTheSame(
            oldItems[oldItemPosition],
            newItems[newItemPosition]
        )
    }
}


interface ItemComparator<in T> {

    fun areItemsTheSame(item1: T, item2: T): Boolean

    fun areContentsTheSame(item1: T, item2: T): Boolean
}