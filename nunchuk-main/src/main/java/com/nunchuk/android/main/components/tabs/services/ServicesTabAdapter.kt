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

package com.nunchuk.android.main.components.tabs.services

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.nunchuk.android.core.util.getString
import com.nunchuk.android.main.R
import com.nunchuk.android.main.databinding.ItemServiceTabCategoryBinding
import com.nunchuk.android.main.databinding.ItemServiceTabEmptyStateBinding
import com.nunchuk.android.main.databinding.ItemServiceTabNonSubHeaderBinding
import com.nunchuk.android.main.databinding.ItemServiceTabNonSubRowBinding
import com.nunchuk.android.main.databinding.ItemServiceTabObserverRoleBinding
import com.nunchuk.android.main.databinding.ItemServiceTabRowBinding
import com.nunchuk.android.main.databinding.ItemServicesTabBannerBinding
import com.nunchuk.android.widget.util.setOnDebounceClickListener

class ServicesTabAdapter constructor(
    val itemClick: (ServiceTabRowItem) -> Unit,
    val bannerClick: (id: String) -> Unit
) :
    ListAdapter<Any, ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceTabViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.item_service_tab_category -> {
                return ServiceTabViewHolder.CategoryViewHolder(
                    ItemServiceTabCategoryBinding.inflate(inflater, parent, false)
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
                viewHolder
            }
            R.layout.item_service_tab_non_sub_row -> {
                val viewHolder = ServiceTabViewHolder.NonSubRowViewHolder(
                    ItemServiceTabNonSubRowBinding.inflate(inflater, parent, false)
                )
                viewHolder
            }
            R.layout.item_services_tab_banner -> {
                val viewHolder = ServiceTabViewHolder.BannerViewHolder(
                    ItemServicesTabBannerBinding.inflate(inflater, parent, false)
                )
                viewHolder.itemView.setOnDebounceClickListener {
                    viewHolder.banner?.let {
                        bannerClick(it.id)
                    }
                }
                viewHolder
            }
            R.layout.item_service_tab_observer_role -> {
                val viewHolder = ServiceTabViewHolder.ObserverRoleViewHolder(
                    ItemServiceTabObserverRoleBinding.inflate(inflater, parent, false)
                )
                viewHolder
            }
            R.layout.item_service_tab_empty_state -> {
                val viewHolder = ServiceTabViewHolder.EmptyStateViewHolder(
                    ItemServiceTabEmptyStateBinding.inflate(inflater, parent, false)
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
            is ServiceTabViewHolder.NonSubHeaderViewHolder -> {
                holder.bind(getItem(position) as NonSubHeader)
            }
            is ServiceTabViewHolder.BannerViewHolder -> {
                holder.bind(getItem(position) as Banner)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ServiceTabRowCategory -> R.layout.item_service_tab_category
            is ServiceTabRowItem -> R.layout.item_service_tab_row
            is NonSubHeader -> R.layout.item_service_tab_non_sub_header
            is NonSubRow -> R.layout.item_service_tab_non_sub_row
            is Banner -> R.layout.item_services_tab_banner
            is ObserverRole -> R.layout.item_service_tab_observer_role
            is EmptyState -> R.layout.item_service_tab_empty_state
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
            oldItem is NonSubHeader && newItem is NonSubHeader -> oldItem.title == newItem.title
            oldItem is Banner && newItem is Banner -> oldItem.id == newItem.id
            oldItem is ObserverRole && newItem is ObserverRole -> oldItem == newItem
            oldItem is EmptyState && newItem is EmptyState -> oldItem == newItem
            else -> false
        }
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is ServiceTabRowCategory && newItem is ServiceTabRowCategory -> oldItem == newItem
            oldItem is ServiceTabRowItem && newItem is ServiceTabRowItem -> oldItem == newItem
            oldItem is NonSubRow && newItem is NonSubRow -> oldItem == newItem
            oldItem is NonSubHeader && newItem is NonSubHeader -> oldItem == newItem
            oldItem is Banner && newItem is Banner -> oldItem == newItem
            oldItem is ObserverRole && newItem is ObserverRole -> oldItem == newItem
            oldItem is EmptyState && newItem is EmptyState -> oldItem == newItem
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
        val binding: ItemServiceTabCategoryBinding
    ) : ServiceTabViewHolder(binding.root) {
        fun bind(item: ServiceTabRowCategory) {
            binding.tvTitle.text = getString(item.title)
            binding.ivLogo.setImageResource(item.drawableId)
        }
    }

    class ObserverRoleViewHolder(
        val binding: ItemServiceTabObserverRoleBinding
    ) : ServiceTabViewHolder(binding.root)

    class EmptyStateViewHolder(
        val binding: ItemServiceTabEmptyStateBinding
    ) : ServiceTabViewHolder(binding.root)

    class BannerViewHolder(val binding: ItemServicesTabBannerBinding) :
        ServiceTabViewHolder(binding.root) {
        internal var banner: Banner? = null
        internal fun bind(item: Banner) {
            banner = item
            binding.containerNonSubscriber.tag = item.id
            Glide.with(binding.ivNonSubscriber)
                .load(item.url)
                .into(binding.ivNonSubscriber)
            binding.tvNonSubscriber.text = item.title
        }
    }

    class NonSubHeaderViewHolder(
        val binding: ItemServiceTabNonSubHeaderBinding
    ) : ServiceTabViewHolder(binding.root) {

        internal fun bind(item: NonSubHeader) {
            binding.tvTitle.text = item.title
            binding.tvDesc.text = item.desc
        }
    }

    class NonSubRowViewHolder(
        val binding: ItemServiceTabNonSubRowBinding
    ) : ServiceTabViewHolder(binding.root) {
        internal fun bind(item: NonSubRow) {
            binding.tvTitle.text = item.title
            binding.tvDesc.text = item.desc
            Glide.with(binding.image)
                .load(item.url)
                .into(binding.image)
        }
    }

}