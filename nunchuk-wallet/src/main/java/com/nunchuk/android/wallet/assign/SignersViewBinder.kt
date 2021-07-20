package com.nunchuk.android.wallet.assign

import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.view.isVisible
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.wallet.R
import com.nunchuk.android.widget.util.AbsViewBinder
import java.util.*

internal class SignersViewBinder(
    container: ViewGroup,
    signers: List<SignerModel>,
    private val selectedXpfs: List<String> = emptyList(),
    val onItemSelectedListener: (String, Boolean) -> Unit,
) : AbsViewBinder<SignerModel>(container, signers) {

    override val layoutId: Int = R.layout.item_assign_signer

    override fun bindItem(position: Int, model: SignerModel) {
        val itemView = container.getChildAt(position)
        val signerName = itemView.findViewById<TextView>(R.id.signerName)
        val avatar = itemView.findViewById<TextView>(R.id.avatar)
        val xfp = itemView.findViewById<TextView>(R.id.xpf)
        val warn = itemView.findViewById<TextView>(R.id.warning)
        val checkBox = itemView.findViewById<CheckBox>(R.id.checkbox)

        val signerType = if (model.software) context.getString(R.string.nc_signer_type_software) else context.getString(R.string.nc_signer_type_air_gapped)
        itemView.findViewById<TextView>(R.id.signerType).text = signerType

        warn.isVisible = model.used
        avatar.text = model.name.shorten().toUpperCase(Locale.getDefault())
        signerName.text = model.name
        val xfpValue = "XFP: ${model.fingerPrint}"
        xfp.text = xfpValue
        checkBox.isChecked = selectedXpfs.isNotEmpty() && selectedXpfs.contains(model.fingerPrint)
        checkBox.setOnCheckedChangeListener { _, checked -> onItemSelectedListener(model.fingerPrint, checked) }
    }
}