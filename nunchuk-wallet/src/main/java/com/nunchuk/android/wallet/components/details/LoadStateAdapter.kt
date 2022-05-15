package com.nunchuk.android.wallet.components.details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.wallet.databinding.PaginationFooterBinding

class LoadStateAdapter : LoadStateAdapter<LoadingViewHolder>() {

    override fun onBindViewHolder(holder: LoadingViewHolder, loadState: LoadState) {
        holder.bindData(loadState)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        loadState: LoadState
    ) = LoadingViewHolder(PaginationFooterBinding.inflate(LayoutInflater.from(parent.context), parent, false))
}

class LoadingViewHolder(
    private val binding: PaginationFooterBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bindData(loadState: LoadState) {
        binding.footerLoader.isVisible = loadState === LoadState.Loading
    }

}
