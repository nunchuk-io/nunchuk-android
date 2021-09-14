package com.nunchuk.android.main.components.tabs.wallet

import android.view.ViewGroup
import androidx.core.view.get
import com.nunchuk.android.core.databinding.ItemWalletBinding
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getUSDAmount
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.wallet.util.bindWalletConfiguration
import com.nunchuk.android.widget.util.AbsViewBinder

internal class WalletsViewBinder(
    container: ViewGroup,
    wallets: List<Wallet>,
    val callback: (String) -> Unit = {}
) : AbsViewBinder<Wallet, ItemWalletBinding>(container, wallets) {

    override fun initializeBinding() = ItemWalletBinding.inflate(inflater, container, false)

    override fun bindItem(position: Int, model: Wallet) {
        val balance = "(${model.getUSDAmount()})"
        val binding = ItemWalletBinding.bind(container[position])
        binding.walletName.text = model.name
        binding.btc.text = model.getBTCAmount()
        binding.balance.text = balance
        binding.config.bindWalletConfiguration(model)
        binding.root.setOnClickListener { callback(model.id) }
    }
}