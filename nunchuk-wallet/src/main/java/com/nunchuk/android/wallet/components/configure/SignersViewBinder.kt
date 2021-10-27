package com.nunchuk.android.wallet.components.configure

import android.view.ViewGroup
import androidx.core.view.get
import androidx.core.view.isVisible
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.core.databinding.ItemAssignSignerBinding
import com.nunchuk.android.widget.util.AbsViewBinder

internal class SignersViewBinder(
    container: ViewGroup,
    signers: List<SignerModel>,
    private val selectedSigners: List<SignerModel> = emptyList(),
    val onItemSelectedListener: (SignerModel, Boolean) -> Unit,
) : AbsViewBinder<SignerModel, ItemAssignSignerBinding>(container, signers) {

    override fun initializeBinding() = ItemAssignSignerBinding.inflate(inflater, container, false)

    override fun bindItem(position: Int, model: SignerModel) {
        val binding = ItemAssignSignerBinding.bind(container[position])
        val signerType = if (model.software) {
            context.getString(R.string.nc_signer_type_software)
        } else {
            context.getString(R.string.nc_signer_type_air_gapped)
        }
        val xfpValue = "XFP: ${model.fingerPrint}"
        binding.signerType.text = signerType
        binding.warning.isVisible = model.used
        binding.avatar.text = model.name.shorten()
        binding.signerName.text = model.name
        binding.xpf.text = xfpValue
        binding.checkbox.isChecked = selectedSigners.isNotEmpty() && (selectedSigners.firstOrNull { it.isSame(model) } != null)
        binding.checkbox.setOnCheckedChangeListener { _, checked -> onItemSelectedListener(model, checked) }
    }
}