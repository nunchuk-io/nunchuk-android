package com.nunchuk.android.wallet.shared.components.assign

import android.view.ViewGroup
import androidx.core.view.get
import androidx.core.view.isVisible
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.core.util.toReadableSignerType
import com.nunchuk.android.wallet.core.databinding.ItemAssignSignerBinding
import com.nunchuk.android.widget.util.AbsViewBinder

internal class SignersViewBinder(
    container: ViewGroup,
    signers: List<SignerModel>,
    private val canSelect: Boolean = true,
    private val selectedXpfs: List<String> = emptyList(),
    val onItemSelectedListener: (String, Boolean) -> Unit,
) : AbsViewBinder<SignerModel, ItemAssignSignerBinding>(container, signers) {

    override fun initializeBinding() = ItemAssignSignerBinding.inflate(inflater, container, false)

    override fun bindItem(position: Int, model: SignerModel) {
        val binding = ItemAssignSignerBinding.bind(container[position])
        val xfpValue = "XFP: ${model.fingerPrint}"
        binding.signerType.text = model.toReadableSignerType(context)
        binding.warning.isVisible = model.used
        binding.avatar.text = model.name.shorten()
        binding.signerName.text = model.name
        binding.xpf.text = xfpValue
        binding.checkbox.isChecked = selectedXpfs.isNotEmpty() && selectedXpfs.contains(model.fingerPrint)
        binding.checkbox.setOnCheckedChangeListener { _, checked -> onItemSelectedListener(model.fingerPrint, checked) }
        if (!binding.checkbox.isChecked) {
            binding.checkbox.isEnabled = canSelect
        }
    }
}