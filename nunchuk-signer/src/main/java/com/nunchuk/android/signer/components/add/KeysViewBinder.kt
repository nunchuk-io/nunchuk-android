package com.nunchuk.android.signer.components.add

import android.view.ViewGroup
import androidx.core.view.get
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.signer.databinding.ItemKeyBinding
import com.nunchuk.android.widget.util.AbsViewBinder

internal class KeysViewBinder(
    container: ViewGroup,
    signers: List<SingleSigner>,
    val callback: (SingleSigner) -> Unit = {}
) : AbsViewBinder<SingleSigner, ItemKeyBinding>(container, signers) {

    override fun initializeBinding() = ItemKeyBinding.inflate(inflater, container, false)

    override fun bindItem(position: Int, model: SingleSigner) {
        val binding = ItemKeyBinding.bind(container[position])
        binding.key.text = model.derivationPath
        binding.root.setOnClickListener {
            callback(model)
        }
    }

}
