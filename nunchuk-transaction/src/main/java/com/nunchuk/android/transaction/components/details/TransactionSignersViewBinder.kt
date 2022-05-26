package com.nunchuk.android.transaction.components.details

import android.view.ViewGroup
import androidx.core.view.get
import androidx.core.view.isVisible
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.core.util.toReadableSignerType
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
        binding.avatar.text = model.name.shorten()
        binding.signerName.text = model.name
        binding.xpf.text = xfpValue
        binding.signerType.text = model.toReadableSignerType(context)
        binding.btnSign.setOnClickListener { listener(model) }
        val isSigned = model.isSigned()
        if (isSigned) {
            binding.btnSign.isVisible = false
            binding.signed.isVisible = true
            binding.signNotAvailable.isVisible = false
        } else if (!model.localKey) {
            binding.btnSign.isVisible = false
            binding.signed.isVisible = false
            binding.signNotAvailable.isVisible = true
        } else {
            binding.btnSign.isVisible = true
            binding.signed.isVisible = false
            binding.signNotAvailable.isVisible = false
        }
    }

    private fun SignerModel.isSigned() = signerMap[fingerPrint] ?: false

}