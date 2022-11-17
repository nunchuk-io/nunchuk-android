/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.messages.components.detail.holder

import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.util.linkify
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.databinding.ItemNunchukBannerNewChatBinding

internal class NunchukBannerNewChatHolder(
    binding: ItemNunchukBannerNewChatBinding,
    private val dismissBannerNewChatListener: () -> Unit,
    private val createSharedWalletListener: () -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.btnNoThanks.setOnClickListener { dismissBannerNewChatListener() }
        binding.btnCreateSharedWallet.setOnClickListener { createSharedWalletListener() }
        binding.introSubtitle.linkify(
            binding.root.context.getString(R.string.nc_text_read_our_guide_linkify),
            "https://resources.nunchuk.io/getting-started" // put it in remote config later
        )
    }

}