package com.nunchuk.android.transaction.details

import android.view.ViewGroup
import androidx.core.view.get
import androidx.core.view.isVisible
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.databinding.ItemTransactionSignerBinding
import com.nunchuk.android.widget.util.AbsViewBinder

internal class TransactionSignersViewBinder(
    container: ViewGroup,
    private val signerMap: Map<String, Boolean>,
    signers: List<SignerModel>,
    val listener: (SignerModel) -> Unit = {}
) : AbsViewBinder<SignerModel, ItemTransactionSignerBinding>(container, signers) {

    override fun initializeBinding() = ItemTransactionSignerBinding.inflate(inflater, container, false)

    override fun bindItem(position: Int, model: SignerModel) {
        val binding = ItemTransactionSignerBinding.bind(container[position])
        val xfpValue = "XFP: ${model.fingerPrint}"
        val signerType = if (model.software) context.getString(R.string.nc_signer_type_software) else context.getString(R.string.nc_signer_type_air_gapped)
        binding.avatar.text = model.name.shorten()
        binding.signerName.text = model.name
        binding.xpf.text = xfpValue
        binding.signerType.text = signerType
        binding.btnSign.setOnClickListener { listener(model) }
        if (signerMap.count { !it.value } > 0) {
            val isSigned = model.isSigned()
            binding.btnSign.isVisible = !isSigned
            binding.signed.isVisible = isSigned
        } else {
            binding.btnSign.isVisible = false
            binding.signed.isVisible = false
        }
    }

    private fun SignerModel.isSigned() = signerMap[fingerPrint] ?: false

}