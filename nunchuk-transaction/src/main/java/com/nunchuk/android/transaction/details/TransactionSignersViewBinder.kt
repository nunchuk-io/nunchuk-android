package com.nunchuk.android.transaction.details

import android.view.ViewGroup
import android.widget.TextView
import com.nunchuk.android.arch.ext.isVisible
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.transaction.R
import com.nunchuk.android.widget.util.AbsViewBinder
import java.util.*

internal class TransactionSignersViewBinder(
    container: ViewGroup,
    private val signerMap: Map<String, Boolean>,
    signers: List<SignerModel>,
    val listener: (SignerModel) -> Unit = {}
) : AbsViewBinder<SignerModel>(container, signers) {

    override val layoutId: Int = R.layout.item_transaction_signer

    override fun bindItem(position: Int, model: SignerModel) {
        container.getChildAt(position).apply {
            val xfpValue = "XFP: ${model.fingerPrint}"
            val signerType = if (model.software) context.getString(R.string.nc_signer_type_software) else context.getString(R.string.nc_signer_type_air_gapped)
            findViewById<TextView>(R.id.avatar).text = model.name.shorten().toUpperCase(Locale.getDefault())
            findViewById<TextView>(R.id.signerName).text = model.name
            findViewById<TextView>(R.id.xpf).text = xfpValue
            findViewById<TextView>(R.id.signerType).text = signerType
            val btnSigned = findViewById<TextView>(R.id.signed)
            val btnSign = findViewById<TextView>(R.id.btnSign)
            btnSign.setOnClickListener { listener(model) }
            val isSigned = model.isSigned()
            btnSign.isVisible = !isSigned
            btnSigned.isVisible = isSigned
        }
    }

    private fun SignerModel.isSigned() = signerMap[fingerPrint] ?: false

}