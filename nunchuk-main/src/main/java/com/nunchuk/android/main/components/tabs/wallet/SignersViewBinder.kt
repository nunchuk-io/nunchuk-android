package com.nunchuk.android.main.components.tabs.wallet

import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.get
import com.nunchuk.android.main.R
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.widget.util.AbsViewBinder

internal class SignersViewBinder(
    container: ViewGroup,
    signers: List<SingleSigner>,
    val onItemClickListener: (SingleSigner) -> Unit = {},
) : AbsViewBinder<SingleSigner>(container, signers) {

    override val layoutId: Int = R.layout.item_signer

    override fun bindItem(position: Int, model: SingleSigner) {
        container[position].apply {
            findViewById<TextView>(R.id.signerName).text = model.name
            val xfpValue = "XFP: ${model.masterFingerprint}"
            findViewById<TextView>(R.id.xpf).text = xfpValue
            setOnClickListener { onItemClickListener(model) }
        }
    }
}