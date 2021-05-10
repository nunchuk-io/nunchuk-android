package com.nunchuk.android.wallet.confirm

import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import com.nunchuk.android.arch.ext.isVisible
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.wallet.R
import com.nunchuk.android.widget.util.AbsViewBinder
import java.util.*

internal class SignersViewBinder(
    container: ViewGroup,
    signers: List<SignerModel>,
) : AbsViewBinder<SignerModel>(container, signers) {

    override val layoutId: Int = R.layout.item_assign_signer

    override fun bindItem(position: Int, model: SignerModel) {
        val itemView = container.getChildAt(position)
        val signerName = itemView.findViewById<TextView>(R.id.signerName)
        val avatar = itemView.findViewById<TextView>(R.id.avatar)
        val xfp = itemView.findViewById<TextView>(R.id.xpf)
        val warn = itemView.findViewById<TextView>(R.id.warning)
        val checkBox = itemView.findViewById<CheckBox>(R.id.checkbox)

        warn.isVisible = model.used
        avatar.text = model.name.shorten().toUpperCase(Locale.getDefault())
        signerName.text = model.name
        val xfpValue = "XFP: ${model.fingerPrint}"
        xfp.text = xfpValue
        checkBox.isVisible = false
    }
}