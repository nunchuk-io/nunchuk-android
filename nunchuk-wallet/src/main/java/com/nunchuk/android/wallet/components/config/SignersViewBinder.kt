/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.wallet.components.config

import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.core.util.toReadableDrawable
import com.nunchuk.android.core.util.toReadableSignerType
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.databinding.ItemWalletConfigSignerBinding
import com.nunchuk.android.widget.util.AbsViewBinder
import com.nunchuk.android.widget.util.setOnDebounceClickListener

internal class SignersViewBinder(
    container: ViewGroup,
    signers: List<SignerModel>,
    private val isInactiveAssistedWallet: Boolean = false,
    private val onViewPolicy: (model: SignerModel) -> Unit = {},
) : AbsViewBinder<SignerModel, ItemWalletConfigSignerBinding>(container, signers) {

    override fun initializeBinding() = ItemWalletConfigSignerBinding.inflate(inflater, container, false)

    override fun bindItem(position: Int, model: SignerModel) {
        val binding = ItemWalletConfigSignerBinding.bind(container.getChildAt(position))

        val isServerKey = model.type == SignerType.SERVER
        binding.btnViewKeyPolicy.isVisible = isServerKey && isInactiveAssistedWallet.not()
        binding.signerType.isVisible = isServerKey.not()
        binding.xpf.isVisible = isServerKey.not() || isInactiveAssistedWallet
        binding.btnViewKeyPolicy.setOnDebounceClickListener { onViewPolicy(model) }

        binding.signerType.text = model.toReadableSignerType(context, isIgnorePrimary = true)
        binding.avatar.isGone = model.localKey
        binding.ivSignerType.isVisible = model.localKey
        if (model.localKey) {
            binding.ivSignerType.setImageDrawable(model.toReadableDrawable(context))
        } else {
            binding.avatar.text = model.name.shorten()
        }
        binding.signerName.text = model.name
        if (isServerKey) {
            binding.xpf.text = context.getString(R.string.nc_inactive)
        } else {
            binding.xpf.text = model.getXfpOrCardIdLabel()
        }
        binding.signerPrimaryKeyType.isVisible = model.isPrimaryKey
        binding.tvBip32Path.isVisible = model.derivationPath.isNotEmpty() && isServerKey.not()
        binding.tvBip32Path.text = "BIP32 path: ${model.derivationPath}"
        binding.tvBip32Path.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,0,0)
    }
}