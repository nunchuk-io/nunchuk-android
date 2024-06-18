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

package com.nunchuk.android.core.wallet

import android.view.ViewGroup
import androidx.core.view.get
import androidx.core.view.isVisible
import com.nunchuk.android.core.R
import com.nunchuk.android.core.databinding.ItemWalletBinding
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.share.wallet.bindWalletConfiguration
import com.nunchuk.android.utils.Utils
import com.nunchuk.android.widget.util.AbsViewBinder

internal class WalletsViewBinder(
    container: ViewGroup,
    wallets: List<WalletExtended>,
    val isAssistedWallet: (String) -> Boolean = { false },
    val isLockedWallet: (String) -> Boolean = { false },
    val lockdownWalletIds: Set<String>,
    private val hideWalletDetail: Boolean = false,
    val callback: (String) -> Unit = {}
) : AbsViewBinder<WalletExtended, ItemWalletBinding>(container, wallets) {

    override fun initializeBinding() = ItemWalletBinding.inflate(inflater, container, false)

    override fun bindItem(position: Int, model: WalletExtended) {
        val isAssistedWallet = isAssistedWallet(model.wallet.id)
        val isLockedWallet = isLockedWallet(model.wallet.id)
        val wallet = model.wallet
        val balance = "(${wallet.getCurrencyAmount()})"
        val binding = ItemWalletBinding.bind(container[position])
        binding.walletName.text = wallet.name

        binding.btc.text = Utils.maskValue(wallet.getBTCAmount(), hideWalletDetail)
        binding.balance.text = Utils.maskValue(balance, hideWalletDetail)
        binding.shareIcon.isVisible = model.isShared || isAssistedWallet || isLockedWallet
        if (isLockedWallet) {
            binding.shareIcon.text = context.getString(R.string.nc_deactivated)
        } else if (isAssistedWallet) {
            binding.shareIcon.text =
                Utils.maskValue(context.getString(R.string.nc_assisted), hideWalletDetail)
        } else {
            binding.shareIcon.text =
                Utils.maskValue(context.getString(R.string.nc_text_shared), hideWalletDetail)
        }
        binding.shareIcon.setCompoundDrawablesWithIntrinsicBounds(
            if (isLockedWallet) 0 else R.drawable.ic_wallet_small,
            0,
            0,
            0
        )
        binding.config.bindWalletConfiguration(wallet, hideWalletDetail)
        binding.root.setOnClickListener {
            if (isLockedWallet) return@setOnClickListener
            callback(wallet.id)
        }
        if (lockdownWalletIds.contains(wallet.id) || isLockedWallet) {
            binding.root.setBackgroundResource(R.drawable.nc_grey_background)
        } else if (isAssistedWallet) {
            binding.root.setBackgroundResource(R.drawable.nc_gradient_premium_background)
        } else {
            binding.root.setBackgroundResource(R.drawable.nc_gradient_background)
        }
    }
}