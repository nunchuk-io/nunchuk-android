package com.nunchuk.android.main.components.tabs.services

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.nunchuk.android.core.util.getString
import com.nunchuk.android.main.R
import com.nunchuk.android.main.databinding.ItemServerTabCategoryBinding
import com.nunchuk.android.main.databinding.ItemServiceTabNonSubHeaderBinding
import com.nunchuk.android.main.databinding.ItemServiceTabNonSubRowBinding
import com.nunchuk.android.main.databinding.ItemServiceTabRowBinding

class ServicesTabAdapter constructor(
    val itemClick: (ServiceTabRowItem) -> Unit,
    val onClaimClick: () -> Unit
) :
    ListAdapter<Any, ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceTabViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.item_server_tab_category -> {
                return ServiceTabViewHolder.CategoryViewHolder(
                    ItemServerTabCategoryBinding.inflate(inflater, parent, false)
                )
            }
            R.layout.item_service_tab_row -> {
                val viewHolder = ServiceTabViewHolder.RowViewHolder(
                    ItemServiceTabRowBinding.inflate(inflater, parent, false)
                )
                viewHolder.itemView.setOnClickListener {
                    viewHolder.data?.let {
                        itemClick(it)
                    }
                }
                viewHolder
            }
            R.layout.item_service_tab_non_sub_header -> {
                val viewHolder = ServiceTabViewHolder.NonSubHeaderViewHolder(
                    ItemServiceTabNonSubHeaderBinding.inflate(inflater, parent, false)
                )
                viewHolder.binding.claimLayout.setOnClickListener {
                    onClaimClick()
                }
                viewHolder
            }
            R.layout.item_service_tab_non_sub_row -> {
                val viewHolder = ServiceTabViewHolder.NonSubRowViewHolder(
                    ItemServiceTabNonSubRowBinding.inflate(inflater, parent, false)
                )
                viewHolder
            }
            else -> throw IllegalStateException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is ServiceTabViewHolder.CategoryViewHolder -> {
                holder.bind(getItem(position) as ServiceTabRowCategory)
            }
            is ServiceTabViewHolder.RowViewHolder -> {
                holder.bind(getItem(position) as ServiceTabRowItem)
            }
            is ServiceTabViewHolder.NonSubRowViewHolder -> {
                holder.bind(getItem(position) as NonSubRow)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ServiceTabRowCategory -> R.layout.item_server_tab_category
            is ServiceTabRowItem -> R.layout.item_service_tab_row
            is NonSubHeader -> R.layout.item_service_tab_non_sub_header
            is NonSubRow -> R.layout.item_service_tab_non_sub_row
            else -> throw IllegalStateException("Unknown view type at position $position")
        }
    }

}

object DiffCallback : DiffUtil.ItemCallback<Any>() {

    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is ServiceTabRowCategory && newItem is ServiceTabRowCategory -> oldItem.title == newItem.title
            oldItem is ServiceTabRowItem && newItem is ServiceTabRowItem -> oldItem.title == newItem.title
            oldItem is NonSubRow && newItem is NonSubRow -> oldItem.title == newItem.title
            else -> false
        }
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is ServiceTabRowCategory && newItem is ServiceTabRowCategory -> oldItem == newItem
            oldItem is ServiceTabRowItem && newItem is ServiceTabRowItem -> oldItem == newItem
            oldItem is NonSubRow && newItem is NonSubRow -> oldItem == newItem
            else -> true
        }
    }
}

sealed class ServiceTabViewHolder(itemView: View) : ViewHolder(itemView) {

    class RowViewHolder(
        val binding: ItemServiceTabRowBinding
    ) : ServiceTabViewHolder(binding.root) {
        var data: ServiceTabRowItem? = null
        fun bind(item: ServiceTabRowItem) {
            this.data = item
            binding.tvName.text = getString(item.title)
        }
    }

    class CategoryViewHolder(
        val binding: ItemServerTabCategoryBinding
    ) : ServiceTabViewHolder(binding.root) {
        fun bind(item: ServiceTabRowCategory) {
            binding.tvTitle.text = getString(item.title)
            binding.ivLogo.setBackgroundResource(item.drawableId)
        }
    }

    class NonSubHeaderViewHolder(
        val binding: ItemServiceTabNonSubHeaderBinding
    ) : ServiceTabViewHolder(binding.root) {

        fun bind(item: NonSubHeader) {
        }
    }

    class NonSubRowViewHolder(
        val binding: ItemServiceTabNonSubRowBinding
    ) : ServiceTabViewHolder(binding.root) {
        fun bind(item: NonSubRow) {
            binding.tvTitle.text = getString(item.title)
            binding.tvDesc.text = getString(item.desc)
            binding.image.setImageDrawable(
                ContextCompat.getDrawable(
                    binding.root.context,
                    item.drawableId
                )
            )
        }
    }

}