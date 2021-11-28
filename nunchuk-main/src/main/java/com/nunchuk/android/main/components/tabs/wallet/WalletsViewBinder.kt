package com.nunchuk.android.main.components.tabs.wallet

import android.view.ViewGroup
import androidx.core.view.get
import androidx.core.view.isVisible
import com.nunchuk.android.core.databinding.ItemWalletBinding
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getUSDAmount
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.wallet.util.bindWalletConfiguration
import com.nunchuk.android.widget.util.AbsViewBinder

internal class WalletsViewBinder(
    container: ViewGroup,
    wallets: List<WalletExtended>,
    val callback: (String) -> Unit = {}
) : AbsViewBinder<WalletExtended, ItemWalletBinding>(container, wallets) {

    override fun initializeBinding() = ItemWalletBinding.inflate(inflater, container, false)

    override fun bindItem(position: Int, model: WalletExtended) {
        val wallet = model.wallet
        val balance = "(${wallet.getUSDAmount()})"
        val binding = ItemWalletBinding.bind(container[position])
        binding.walletName.text = wallet.name
        binding.btc.text = wallet.getBTCAmount()
        binding.balance.text = balance
        binding.shareIcon.isVisible = model.isShared
        binding.config.bindWalletConfiguration(wallet)
        binding.root.setOnClickListener { callback(wallet.id) }
    }
}