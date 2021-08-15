package com.nunchuk.android.wallet.components.config

import android.view.ViewGroup
import androidx.core.view.isVisible
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.core.databinding.ItemAssignSignerBinding
import com.nunchuk.android.widget.util.AbsViewBinder

internal class SignersViewBinder(
    container: ViewGroup,
    signers: List<SignerModel>,
) : AbsViewBinder<SignerModel, ItemAssignSignerBinding>(container, signers) {

    override fun initializeBinding() = ItemAssignSignerBinding.inflate(inflater, container, false)

    override fun bindItem(position: Int, model: SignerModel) {
        val binding = ItemAssignSignerBinding.bind(container.getChildAt(position))
        val signerType = if (model.software) {
            context.getString(R.string.nc_signer_type_software)
        } else {
            context.getString(R.string.nc_signer_type_air_gapped)
        }
        binding.signerType.text = signerType

        binding.warning.isVisible = model.used
        binding.avatar.text = model.name.shorten()
        binding.signerName.text = model.name
        val xfpValue = "XFP: ${model.fingerPrint}"
        binding.xpf.text = xfpValue
        binding.checkbox.isVisible = false
    }

}