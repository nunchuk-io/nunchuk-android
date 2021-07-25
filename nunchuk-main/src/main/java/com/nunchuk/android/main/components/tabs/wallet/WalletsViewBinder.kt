package com.nunchuk.android.main.components.tabs.wallet

import android.view.ViewGroup
import androidx.core.view.get
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getConfiguration
import com.nunchuk.android.core.util.getUSDAmount
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.wallet.databinding.ItemWalletBinding
import com.nunchuk.android.widget.util.AbsViewBinder

internal class WalletsViewBinder(
    container: ViewGroup,
    wallets: List<Wallet>,
    val callback: (String) -> Unit = {}
) : AbsViewBinder<Wallet, ItemWalletBinding>(container, wallets) {

    override fun initializeBinding() = ItemWalletBinding.inflate(inflater, container, false)

    override fun bindItem(position: Int, model: Wallet) {
        val binding = ItemWalletBinding.bind(container[position])
        binding.walletName.text = model.name
        binding.btc.text = model.getBTCAmount()
        binding.balance.text = model.getUSDAmount()
        binding.config.text = model.getConfiguration()
        binding.root.setOnClickListener { callback(model.id) }
    }
}