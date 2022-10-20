package com.nunchuk.android.wallet.components.configure

import android.view.ViewGroup
import androidx.core.view.get
import androidx.core.view.isVisible
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.toReadableDrawable
import com.nunchuk.android.core.util.toReadableSignerType
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.core.databinding.ItemAssignSignerBinding
import com.nunchuk.android.widget.util.AbsViewBinder
import com.nunchuk.android.widget.util.setOnDebounceClickListener

internal class SignersViewBinder(
    container: ViewGroup,
    signers: List<SignerModel>,
    private val selectedSigners: Set<SignerModel> = emptySet(),
    val onItemSelectedListener: (SignerModel, Boolean) -> Unit,
    val onEditPath: (SignerModel) -> Unit,
    private val isShowPath: Boolean,
) : AbsViewBinder<SignerModel, ItemAssignSignerBinding>(container, signers) {

    override fun initializeBinding() = ItemAssignSignerBinding.inflate(inflater, container, false)

    override fun bindItem(position: Int, model: SignerModel) {
        val binding = ItemAssignSignerBinding.bind(container[position])
        val xfpValue = "XFP: ${model.fingerPrint}"
        binding.signerType.text = model.toReadableSignerType(context, isIgnorePrimary = true)
        binding.ivSignerType.isVisible = true
        binding.ivSignerType.setImageDrawable(model.type.toReadableDrawable(context))
        binding.signerName.text = model.name
        binding.xpf.text = xfpValue
        binding.tvBip32Path.isVisible = model.isMasterSigner && model.derivationPath.isNotEmpty() && isShowPath
        binding.tvBip32Path.text = "BIP32 path: ${model.derivationPath}"
        if (model.isEditablePath.not()) {
            binding.tvBip32Path.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,0,0)
            binding.tvBip32Path.setOnDebounceClickListener {  }
        } else {
            binding.tvBip32Path.setOnDebounceClickListener { onEditPath(model) }
            binding.tvBip32Path.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0, R.drawable.ic_edit_small,0)
        }
        binding.checkbox.isChecked =
            selectedSigners.isNotEmpty() && selectedSigners.contains(model)
        binding.checkbox.setOnClickListener {
            onItemSelectedListener(
                model,
                binding.checkbox.isChecked
            )
        }
        binding.signerPrimaryKeyType.isVisible = model.isPrimaryKey
    }
}