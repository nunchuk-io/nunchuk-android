package com.nunchuk.android.messages.components.detail.holder

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.util.linkify
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.databinding.ItemNunchukBannerNewChatBinding

internal class NunchukBannerNewChatHolder(
    private val binding: ItemNunchukBannerNewChatBinding,
    private val dismissBannerNewChatListener: () -> Unit,
    private val createSharedWalletListener: () -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(show: Boolean) {
        binding.bannerContainer.isVisible = show
        binding.btnNoThanks.setOnClickListener { dismissBannerNewChatListener() }
        binding.btnCreateSharedWallet.setOnClickListener { createSharedWalletListener() }
        binding.introSubtitle.linkify(
            binding.root.context.getString(R.string.nc_text_read_our_guide_linkify),
            "https://nunchuk.medium.com" // put it in remote config later
        )
    }

}