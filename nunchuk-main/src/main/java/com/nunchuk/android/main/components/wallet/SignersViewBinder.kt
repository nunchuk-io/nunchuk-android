package com.nunchuk.android.main.components.wallet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nunchuk.android.main.databinding.ItemSignerBinding
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.widget.util.AbsViewBinder

class SignersViewBinder(
    container: ViewGroup,
    signers: List<SingleSigner>,
    val onItemClickListener: (SingleSigner) -> Unit = {}
) : AbsViewBinder<SingleSigner>(container, signers) {

    override fun bindItem(model: SingleSigner): View {
        val binding = ItemSignerBinding.inflate(LayoutInflater.from(container.context), container, false)
        binding.signerName.text = model.name
        binding.xpf.text = model.masterFingerprint
        binding.root.setOnClickListener { onItemClickListener(model) }
        return binding.root
    }
}