package com.nunchuk.android.main.components.tabs.wallet

import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.get
import com.nunchuk.android.main.R
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.widget.util.AbsViewBinder

internal class WalletsViewBinder(
    container: ViewGroup,
    signers: List<Wallet>
) : AbsViewBinder<Wallet>(container, signers) {

    override val layoutId: Int = R.layout.item_wallet

    override fun bindItem(position: Int, model: Wallet) {
        val itemView = container[position]
        itemView.findViewById<TextView>(R.id.walletName).text = model.name
        itemView.findViewById<TextView>(R.id.btc).text = "0.00 BTC"
        itemView.findViewById<TextView>(R.id.balance).text = "($ ${model.balance})"
        itemView.findViewById<TextView>(R.id.config).text = "${model.totalRequireSigns}/${model.signers.size}"
    }
}