package com.nunchuk.android.core.base

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import java.lang.ref.WeakReference

abstract class RecyclerAdapter<VH : RecyclerAdapter.BaseViewHolder<*, T>, T>(
    private var onItemClickListener: OnItemClickListener<T>? = null,
    private var comparator: ItemComparator<T>? = null
) : RecyclerView.Adapter<VH>() {

    private var _items = ArrayList<T>()

    private val items: List<T> get() = _items

    private var job: Job? = null

    private var _adapterDataObserver: RecyclerView.AdapterDataObserver? = null

    fun getItem(pos: Int): T? = items.getOrNull(pos)

    @Volatile
    var recyclerViewReference: WeakReference<RecyclerView>? = null

    protected abstract fun getViewHolder(parent: ViewGroup, viewType: Int): VH?

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerViewReference = WeakReference(recyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val viewHolder = getViewHolder(parent, viewType)
        if (viewHolder != null && onItemClickListener != null) {
            viewHolder.binding.root.setOnClickListener {
                val pos = viewHolder.adapterPosition
                if (pos != NO_POSITION) {
                    onItemClickListener?.onItemClick(items[pos], pos)
                }
            }
        }
        return viewHolder!!
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun registerDataObserver(adapterDataObserver: RecyclerView.AdapterDataObserver) {
        if (_adapterDataObserver != null)
            unregisterAdapterDataObserver(_adapterDataObserver!!)
        _adapterDataObserver = adapterDataObserver
        registerAdapterDataObserver(_adapterDataObserver!!)
    }

    private fun unRegisterDataObserver() {
        if (_adapterDataObserver != null)
            unregisterAdapterDataObserver(_adapterDataObserver!!)
        _adapterDataObserver = null
    }

    open fun updateItems(items: List<T>) {
        if (this.items.isNotEmpty() && items.isNotEmpty() && comparator != null) {
            updateDiffItemsOnly(items)
        } else {
            updateAllItems(items)
        }
    }

    private fun updateAllItems(items: List<T>) {
        updateItemsInModel(items)
        notifyDataSetChanged()
    }

    private fun updateDiffItemsOnly(items: List<T>) {
        if (job != null && !job!!.isCancelled) {
            job!!.cancel()
        }
        job = flow { emit(calculateDiff(items)) }
            .flowOn(Dispatchers.IO)
            .map {
                updateItemsInModel(items)
                it
            }
            .onEach { result -> updateAdapterWithDiffResult(result) }
            .launchIn(CoroutineScope(Dispatchers.Main))
    }

    private fun calculateDiff(newItems: List<T>): DiffUtil.DiffResult {
        return DiffUtil.calculateDiff(DiffUtilCallback(items, newItems, comparator!!))
    }

    private fun updateItemsInModel(items: List<T>) {
        _items.clear()
        _items.addAll(items)
    }

    private fun updateAdapterWithDiffResult(result: DiffUtil.DiffResult) {
        result.dispatchUpdatesTo(this)
        recyclerViewReference?.get()?.layoutManager?.let {
            when (it) {
                is LinearLayoutManager -> {
                    if (it.findFirstCompletelyVisibleItemPosition() == 0) {
                        recyclerViewReference?.get()?.scrollToPosition(0)
                    }
                }
            }
        }
    }

    protected open fun getData() = items

    open fun release() {
        onItemClickListener = null
        unRegisterDataObserver()
    }

    open class BaseViewHolder<out V : ViewBinding, in T>(val binding: V) : RecyclerView.ViewHolder(binding.root) {
        open fun bind(data: T) {}
    }

}

interface OnItemClickListener<in T> {
    fun onItemClick(data: T, position: Int)
}